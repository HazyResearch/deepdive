import scala.collection.mutable.ListBuffer

object DeepDiveLogDeltaDeriver{

  // Default prefix for incremental tables
  val deltaPrefix = "dd_delta_"

  def transfer(stmt: Statement): List[Statement] = stmt match {
    case s: SchemaDeclaration   => transfer(s)
    case s: FunctionDeclaration => transfer(s)
    case s: ExtractionRule      => transfer(s)
    case s: FunctionCallRule    => transfer(s)
    case s: InferenceRule       => transfer(s)
  }

  // Incremental scheme declaration,
  // keep the original scheme and create one delta scheme
  def transfer(stmt: SchemaDeclaration): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    incrementalStatement += stmt
    var newTerms = new ListBuffer[Variable]()
    for (term <- stmt.a.terms) {
      newTerms += Variable(term.varName, deltaPrefix + term.relName, term.index)
    }
    incrementalStatement += SchemaDeclaration(Attribute(deltaPrefix + stmt.a.name, newTerms.toList, stmt.a.types), stmt.isQuery)
    incrementalStatement.toList
  }

  // Incremental function declaration,
  // create one delta function scheme based on original function scheme
  def transfer(stmt: FunctionDeclaration): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    var newTerms = new ListBuffer[Variable]()
    var newInputType: RelationType = stmt.inputType match {
      case inTy: RelationTypeDeclaration => {
        var newNames = new ListBuffer[String]()
        for (name <- inTy.names)
          newNames += deltaPrefix + name
        RelationTypeDeclaration(newNames.toList, inTy.types)
      }
      case inTy: RelationTypeAlias => RelationTypeAlias(deltaPrefix + inTy.likeRelationName)
    }
    var newOutputType: RelationType = stmt.outputType match {
      case outTy: RelationTypeDeclaration => {
        var newNames = new ListBuffer[String]()
        for (name <- outTy.names)
          newNames += deltaPrefix + name
        RelationTypeDeclaration(newNames.toList, outTy.types)
      }
      case outTy: RelationTypeAlias => RelationTypeAlias(deltaPrefix + outTy.likeRelationName)
    }
    incrementalStatement += FunctionDeclaration(stmt.functionName, newInputType, newOutputType, stmt.implementations)
    incrementalStatement.toList
  }

  // Incremental extraction rule,
  // create delta rules based on original extraction rule
  def transfer(stmt: ExtractionRule): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    
    // New head
    var newStmtCqHeadTerms = new ListBuffer[Variable]()
    for (headTerm <- stmt.q.head.terms) {
      newStmtCqHeadTerms += Variable(headTerm.varName, deltaPrefix + headTerm.relName, headTerm.index)
    }
    var newStmtCqHead = Atom(deltaPrefix + stmt.q.head.name, newStmtCqHeadTerms.toList)

    var deltaStmtCqBody = new ListBuffer[Atom]()
    for (stmtCqBody <- stmt.q.body) { // List[Atom]
      var stmtCqBodyTerms = new ListBuffer[Variable]()
      for (bodyTerm <- stmtCqBody.terms) {
        stmtCqBodyTerms += Variable(bodyTerm.varName, deltaPrefix + bodyTerm.relName, bodyTerm.index)
      }
      deltaStmtCqBody += Atom(deltaPrefix + stmtCqBody.name, stmtCqBodyTerms.toList)
    }

    // New body
    var i = 0
    var j = 0
    for (i <- 1 to ((1 << stmt.q.body.length) - 1)) {
      var newStmtCqBody = new ListBuffer[Atom]()
      for (j <- 0 to (stmt.q.body.length - 1)) {
        if ((i & (1 << j)) == 0)
          newStmtCqBody += stmt.q.body(j)
        else
          newStmtCqBody += deltaStmtCqBody(j)
      }
      incrementalStatement += ExtractionRule(ConjunctiveQuery(newStmtCqHead, newStmtCqBody.toList))
    }
    incrementalStatement.toList
  }

  // Incremental function call rule,
  // modify function input and output
  def transfer(stmt: FunctionCallRule): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    incrementalStatement += FunctionCallRule(deltaPrefix + stmt.input, deltaPrefix + stmt.output, stmt.function)
    incrementalStatement.toList
  }

  // Incremental inference rule,
  // create delta rules based on original extraction rule
  def transfer(stmt: InferenceRule): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    
    // New head
    var newStmtCqHeadTerms = new ListBuffer[Variable]()
    for (headTerm <- stmt.q.head.terms) {
      newStmtCqHeadTerms += Variable(headTerm.varName, deltaPrefix + headTerm.relName, headTerm.index)
    }
    var newStmtCqHead = Atom(deltaPrefix + stmt.q.head.name, newStmtCqHeadTerms.toList)

    var deltaStmtCqBody = new ListBuffer[Atom]()
    for (stmtCqBody <- stmt.q.body) { // List[Atom]
      var stmtCqBodyTerms = new ListBuffer[Variable]()
      for (bodyTerm <- stmtCqBody.terms) {
        stmtCqBodyTerms += Variable(bodyTerm.varName, deltaPrefix + bodyTerm.relName, bodyTerm.index)
      }
      deltaStmtCqBody += Atom(deltaPrefix + stmtCqBody.name, stmtCqBodyTerms.toList)
    }

    // New body
    var i = 0
    var j = 0
    for (i <- 1 to ((1 << stmt.q.body.length) - 1)) {
      var newStmtCqBody = new ListBuffer[Atom]()
      for (j <- 0 to (stmt.q.body.length - 1)) {
        if ((i & (1 << j)) == 0)
          newStmtCqBody += stmt.q.body(j)
        else
          newStmtCqBody += deltaStmtCqBody(j)
      }
      incrementalStatement += InferenceRule(ConjunctiveQuery(newStmtCqHead, newStmtCqBody.toList), stmt.weights, stmt.supervision)
    }
    incrementalStatement.toList
  }

  def derive(program: DeepDiveLog.Program): DeepDiveLog.Program = {
    var incrementalProgram = new ListBuffer[Statement]()
    for (x <- program) {
      incrementalProgram = incrementalProgram ++ transfer(x)
    }
    incrementalProgram.toList
  }
}
