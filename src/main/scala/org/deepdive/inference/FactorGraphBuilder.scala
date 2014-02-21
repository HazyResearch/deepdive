package org.deepdive.inference

import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import anorm._
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import org.deepdive.Context
import org.deepdive.extraction.datastore._
import org.deepdive.settings._
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.util.{Random, Try, Success, Failure}

object FactorGraphBuilder {

  // Implementation of FactorGraphBuilder using postgres components
  // TODO: Refactor this
  class PostgresFactorGraphBuilder(val variableSchema: Map[String, String]) 
    extends FactorGraphBuilder with PostgresExtractionDataStoreComponent 
    with PostgresInferenceDataStoreComponent

  // Messages
  sealed trait Message
  case class AddFactorsAndVariables(factorDesc: FactorDesc, holdoutFraction: Double, 
    batchSize: Option[Int]) extends Message
  // case class AddVariables(relation: Relation, fields: Set[String]) extends Message
  case class ProcessRowResult(variables: Seq[Variable], weight: Weight, factor: Factor)
  // case class AddVariablesResult(name: String, success: Boolean)
}

trait FactorGraphBuilder extends Actor with ActorLogging { 
  self: ExtractionDataStoreComponent with InferenceDataStoreComponent =>
  
  def variableSchema: Map[String, String]

  val variableOffsetMap = variableSchema.keys.toList.sorted.zipWithIndex.toMap

  import FactorGraphBuilder._
  import context.dispatcher

  val rng = new Random(1337)
  val factorIdCounter = new AtomicInteger
  val weightIdCounter = new AtomicInteger
  val variableIdCounter = new AtomicInteger
  // Keeps track of weights
  val weights = new ConcurrentHashMap[String, java.lang.Long]()
  val variableIdMap = new ConcurrentHashMap[Long, java.lang.Long]()

  // We keep track of the variables, weights and factors already added
  // These will be kept in memory at all times.
  // TODO: Ideally, we don't keep anything in memory and resolve conflicts in the database.
  val variableIdSet = Collections.newSetFromMap[Long](
    new ConcurrentHashMap[Long, java.lang.Boolean]())
  val weightIdSet = Collections.newSetFromMap[Long](
    new ConcurrentHashMap[Long, java.lang.Boolean]())
  val factorIdSet = Collections.newSetFromMap[Long](
    new ConcurrentHashMap[Long, java.lang.Boolean]())

  // TODO: Make this parameters tunable
  lazy val parallelism = Runtime.getRuntime.availableProcessors.toInt

  override def preStart() {
    log.info("Starting")
    variableIdSet.clear()
    weightIdSet.clear()
    factorIdSet.clear()
    weights.clear()
    variableIdMap.clear()
    weightIdCounter.set(0)
    factorIdCounter.set(0)
    variableIdCounter.set(0)
  }

  def receive = {
    case AddFactorsAndVariables(factorDesc, holdoutFraction, batchSize) =>
      log.info(s"Processing factor_name=${factorDesc.name} with holdout_faction=${holdoutFraction}")
      Try(addFactorsAndVariables(factorDesc, holdoutFraction, batchSize)) match {
        case Success(x) => sender ! x
        case Failure(exception) => 
          sender ! akka.actor.Status.Failure(exception)
      }
    case _ => 
  }


  def addFactorsAndVariables(factorDesc: FactorDesc, holdoutFraction: Double, 
    batchSize: Option[Int]) {
    // Query the data store..
    dataStore.queryAsMap(factorDesc.inputQuery) { dataIterator =>
      // If the user or the data store defines a batch size we use that.
      val chosenBatchSize = batchSize orElse inferenceDataStore.BatchSize
      val batchIterator = chosenBatchSize match {
        case Some(x) => dataIterator.grouped(x)
        case None => Iterator(dataIterator)
      }

      batchIterator.foreach { group =>
        // Group by parallelism
        val parallelGroupSize = Math.max((group.size / parallelism).toInt, 1)
        val tasks = group.toList.grouped(parallelGroupSize).map { case rows =>
          Future { 
            log.info(s"processing num_rows=${rows.size}")
            val res = rows.map { row => processRow(row, factorDesc, holdoutFraction) }
            res
          }.mapTo[List[ProcessRowResult]]
        }
        val mergedResults = Future.sequence(tasks)
        // Wait for result from all threads and flush the datastore
        val results = Await.result(mergedResults, 1337.hours).toList
        for (result <- results.flatten) {
          if (weightIdSet.add(result.weight.id)) inferenceDataStore.addWeight(result.weight)
          result.variables.foreach { v => if (variableIdSet.add(v.id)) inferenceDataStore.addVariable(v) }
          if (factorIdSet.add(result.factor.id)) inferenceDataStore.addFactor(result.factor)
        }
        
        log.debug(s"flushing data for factor_name=${factorDesc.name}.")
        inferenceDataStore.flush()
      }
    }
  }

  def processRow(row: Map[String, Any], factorDesc: FactorDesc, holdoutFraction: Double) : ProcessRowResult = {
    val newVariables = buildVariablesForRow(row, factorDesc, holdoutFraction)
    val newWeight = buildWeightForRow(row, factorDesc)
    val newFactor = buildFactorForRow(row, factorDesc, newWeight)
    ProcessRowResult(newVariables, newWeight, newFactor)
  }

  def buildVariablesForRow(rowMap: Map[String, Any], factorDesc: FactorDesc,
    holdoutFraction: Double) = {
    val variableColumns = factorDesc.func.variables.toList
    val variableLocalIds = variableColumns.map { varColumn =>
      Try(inferenceDataStore.getLocalVariableIds(rowMap, varColumn)) match {
        case Failure(ex) =>
          val errorStr = s"Could not find ${varColumn} or ${varColumn.relation}.id. Available columns: ${rowMap.keys.mkString(", ")}." 
          log.error(ex.toString)
          throw new RuntimeException(s"${errorStr} (${ex.toString})")
        case Success(x) => x
      }
    }
    // Ugly matching because different data stores return different values.
    val variableValues = variableColumns.map { varColumn =>
      rowMap.get(varColumn.toString) match {
        case Some(None) => None
        case Some(Some(x)) => Option(x)
        case Some(x) => Option(x)
        case None => None
        case x => Option(x)
      }
    }

    (variableColumns, variableLocalIds, variableValues).zipped
      .flatMap { case(varColumn, localVarIds, varValue) => 
        // Flip a coin to check if the variable should part of the holdout
        val isEvidence = varValue.isDefined
        val isHoldout = isEvidence && (rng.nextDouble() < holdoutFraction)
        val isQuery = !isEvidence || (isEvidence && isHoldout)

        val evidenceValue = varValue match {
          case Some(x: Boolean) => if (x) Option(1.0) else Option(0.0)
          case _ => None
        }

        // Build the variable, one for each ID
        localVarIds.map { localVarId =>
          val globalVariableId = generateVariableId(localVarId, varColumn.key)
          Variable(globalVariableId, VariableDataType.withName(factorDesc.func.variableDataType), 
             evidenceValue, !isQuery, isQuery, varColumn.headRelation, varColumn.field, localVarId)
        }
      }
  }

  def buildWeightForRow(rowMap: Map[String, Any], factorDesc: FactorDesc) = {
    // Find values for the variables in the weight
    val factorWeightValues = factorDesc.weight.variables.map { key => 
      rowMap.get(key).getOrElse {
        val errorMsg = s"Could not find key=${key}. Available keys: ${rowMap.keySet.mkString(", ")}"
        log.error(errorMsg)
        throw new RuntimeException(errorMsg)
      }
    }.collect { 
      case Some(x) => x.toString
      case None => ""
    }

    // Generate a unique ID for the weight
    // TODO: We generate a unique weight Id based on the String hash code. 
    // Can we really be that sure this is unique enough?
    val weightIdentifier = factorDesc.weightPrefix + "_" + factorWeightValues.mkString
    // TODO: Parallelize
    weights.synchronized {
      if (weights.putIfAbsent(weightIdentifier, weightIdCounter.get) == null) {
        weightIdCounter.incrementAndGet()
      }
    }
    val weightId = weights.get(weightIdentifier)

    // Build a human-friendly description fo the weight
    val valueAssignments = factorWeightValues.mkString("=")
    val weightDescription = s"${factorDesc.name}(${valueAssignments})"

    // Find the initial value for the weight
    val weightValue = factorDesc.weight match { 
      case x : KnownFactorWeight => x.value
      case _ => 0.0
    }

    Weight(weightId, weightValue, factorDesc.weight.isInstanceOf[KnownFactorWeight], 
      weightDescription)
  }

  def buildFactorForRow(rowMap: Map[String, Any], factorDesc: FactorDesc, weight: Weight) = {
    val newFactorId = factorIdCounter.getAndIncrement()
    val factorVariables = factorDesc.func.variables.flatMap { factorVar =>
      inferenceDataStore.getLocalVariableIds(rowMap, factorVar).map { localId => 
        (factorVar, localId)
      }
    }.zipWithIndex.map { case((factorVar, localId), position) =>
      val globalId = generateVariableId(localId, factorVar.key)
      FactorVariable(newFactorId.toLong, position, !factorVar.isNegated, globalId)
    }
    Factor(newFactorId, factorDesc.func.getClass.getSimpleName, weight.id, factorVariables.toList)
  }

  def generateVariableId(localId: Long, key: String) : Long = {
    val mappedId = (localId * variableOffsetMap.size) + variableOffsetMap.get(key).getOrElse {
      val errorMsg = s"${key} not found in variable definitions. " + 
        s"Available variables: ${variableOffsetMap.keySet.mkString(",")}"
      throw new RuntimeException(errorMsg)
    }
    // TODO: Parallelize
    variableIdMap.synchronized {
      if (variableIdMap.putIfAbsent(mappedId, variableIdCounter.get) == null) {
        variableIdCounter.incrementAndGet()
      }
    }
    return variableIdMap.get(mappedId)
  }


}