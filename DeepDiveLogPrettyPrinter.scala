// Pretty printer that simply prints the parsed input
object DeepDiveLogPrettyPrinter extends DeepDiveLogHandler {
  override def run(parsedProgram: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    val programToPrint =
      // derive the delta rules for incremental version
      if (config.isIncremental) DeepDiveLogDeltaDeriver.derive(parsedProgram)
      else parsedProgram

    // TODO pretty print in original syntax
    println(programToPrint)
  }
}
