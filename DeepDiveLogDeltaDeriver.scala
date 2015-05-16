import scala.collection.mutable.ListBuffer

object DeepDiveLogDeltaDeriver{

  // Default prefix for incremental tables
  val deltaPrefix = "dd_delta_"
  val newPrefix = "dd_new_"

  def transform(stmt: Statement): List[Statement] = stmt match {
    case s: SchemaDeclaration   => transform(s)
    case s: FunctionDeclaration => transform(s)
    case s: ExtractionRule      => transform(s)
    case s: FunctionCallRule    => transform(s)
    case s: InferenceRule       => transform(s)
  }

  // Incremental scheme declaration,
  // keep the original scheme and create one delta scheme
  def transform(stmt: SchemaDeclaration): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    // Origin table
    incrementalStatement += stmt
    // Delta table
    var deltaTerms = new ListBuffer[Variable]()
    for (term <- stmt.a.terms) {
      deltaTerms += Variable(term.varName, deltaPrefix + term.relName, term.index)
    }
    var deltaStmt = SchemaDeclaration(Attribute(deltaPrefix + stmt.a.name, deltaTerms.toList, stmt.a.types), stmt.isQuery)
    
    incrementalStatement += deltaStmt
    // New table
    val newTerms = new ListBuffer[Variable]()
    for (term <- stmt.a.terms) {
      newTerms += Variable(term.varName, newPrefix + term.relName, term.index)
    }
    var newStmt = SchemaDeclaration(Attribute(newPrefix + stmt.a.name, newTerms.toList, stmt.a.types), stmt.isQuery)
    incrementalStatement += newStmt
    if (!stmt.isQuery) {
      incrementalStatement += ExtractionRule(ConjunctiveQuery(Atom(newStmt.a.name, newStmt.a.terms.toList), List(Atom(stmt.a.name, stmt.a.terms.toList))))
      incrementalStatement += ExtractionRule(ConjunctiveQuery(Atom(newStmt.a.name, newStmt.a.terms.toList), List(Atom(deltaStmt.a.name, deltaStmt.a.terms.toList))))
    }
    incrementalStatement.toList
  }

  // Incremental function declaration,
  // create one delta function scheme based on original function scheme
  def transform(stmt: FunctionDeclaration): List[Statement] = {
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
  def transform(stmt: ExtractionRule): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    
    // New head
    var newStmtCqHeadTerms = new ListBuffer[Variable]()
    for (headTerm <- stmt.q.head.terms) {
      newStmtCqHeadTerms += Variable(headTerm.varName, deltaPrefix + headTerm.relName, headTerm.index)
    }
    var newStmtCqHead = Atom(deltaPrefix + stmt.q.head.name, newStmtCqHeadTerms.toList)
    // dd delta table from dd_delta_ table
    var ddDeltaStmtCqBody = new ListBuffer[Atom]()
    for (stmtCqBody <- stmt.q.body) { // List[Atom]
      var stmtCqBodyTerms = new ListBuffer[Variable]()
      for (bodyTerm <- stmtCqBody.terms) {
        stmtCqBodyTerms += Variable(bodyTerm.varName, deltaPrefix + bodyTerm.relName, bodyTerm.index)
      }
      ddDeltaStmtCqBody += Atom(deltaPrefix + stmtCqBody.name, stmtCqBodyTerms.toList)
    }
    // dd new body from dd_new_ table
    var ddNewStmtCqBody = new ListBuffer[Atom]()
    for (stmtCqBody <- stmt.q.body) { // List[Atom]
      var stmtCqBodyTerms = new ListBuffer[Variable]()
      for (bodyTerm <- stmtCqBody.terms) {
        stmtCqBodyTerms += Variable(bodyTerm.varName, newPrefix + bodyTerm.relName, bodyTerm.index)
      }
      ddNewStmtCqBody += Atom(newPrefix + stmtCqBody.name, stmtCqBodyTerms.toList)
    }

    // New statement
    var i = 0
    var j = 0
    for (i <- 0 to (stmt.q.body.length - 1)) {
      var newStmtCqBody = new ListBuffer[Atom]()
      for (j <- 0 to (stmt.q.body.length - 1)) {
        if (j > i)
          newStmtCqBody += stmt.q.body(j)
        else if (j < i)
          newStmtCqBody += ddNewStmtCqBody(j)
          else if (j == i)
            newStmtCqBody += ddDeltaStmtCqBody(j)
      }
      incrementalStatement += ExtractionRule(ConjunctiveQuery(newStmtCqHead, newStmtCqBody.toList))
    }
    incrementalStatement.toList
  }

  // Incremental function call rule,
  // modify function input and output
  def transform(stmt: FunctionCallRule): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    incrementalStatement += FunctionCallRule(deltaPrefix + stmt.input, deltaPrefix + stmt.output, stmt.function)
    incrementalStatement.toList
  }

  // Incremental inference rule,
  // create delta rules based on original extraction rule
  def transform(stmt: InferenceRule): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    
    // New head
    var newStmtCqHeadTerms = new ListBuffer[Variable]()
    for (headTerm <- stmt.q.head.terms) {
      newStmtCqHeadTerms += Variable(headTerm.varName, deltaPrefix + headTerm.relName, headTerm.index)
    }
    var newStmtCqHead = Atom(deltaPrefix + stmt.q.head.name, newStmtCqHeadTerms.toList)
    // dd delta table from dd_delta_ table
    var ddDeltaStmtCqBody = new ListBuffer[Atom]()
    for (stmtCqBody <- stmt.q.body) { // List[Atom]
      var stmtCqBodyTerms = new ListBuffer[Variable]()
      for (bodyTerm <- stmtCqBody.terms) {
        stmtCqBodyTerms += Variable(bodyTerm.varName, deltaPrefix + bodyTerm.relName, bodyTerm.index)
      }
      ddDeltaStmtCqBody += Atom(deltaPrefix + stmtCqBody.name, stmtCqBodyTerms.toList)
    }
    // dd new body from dd_new_ table
    var ddNewStmtCqBody = new ListBuffer[Atom]()
    for (stmtCqBody <- stmt.q.body) { // List[Atom]
      var stmtCqBodyTerms = new ListBuffer[Variable]()
      for (bodyTerm <- stmtCqBody.terms) {
        stmtCqBodyTerms += Variable(bodyTerm.varName, newPrefix + bodyTerm.relName, bodyTerm.index)
      }
      ddNewStmtCqBody += Atom(newPrefix + stmtCqBody.name, stmtCqBodyTerms.toList)
    }

    // New statement
    var i = 0
    var j = 0
    for (i <- 0 to (stmt.q.body.length - 1)) {
      var newStmtCqBody = new ListBuffer[Atom]()
      for (j <- 0 to (stmt.q.body.length - 1)) {
        if (j > i)
          newStmtCqBody += stmt.q.body(j)
        else if (j < i)
          newStmtCqBody += ddNewStmtCqBody(j)
          else if (j == i)
            newStmtCqBody += ddDeltaStmtCqBody(j)
      }
      incrementalStatement += InferenceRule(ConjunctiveQuery(newStmtCqHead, newStmtCqBody.toList), stmt.weights, stmt.supervision)
    }
    incrementalStatement.toList
  }

  def derive(program: DeepDiveLog.Program): DeepDiveLog.Program = {
    var incrementalProgram = new ListBuffer[Statement]()
    for (x <- program) {
      incrementalProgram = incrementalProgram ++ transform(x)
    }
    incrementalProgram.toList
  }
}
