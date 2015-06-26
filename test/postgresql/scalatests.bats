#!/usr/bin/env bats
# DeepDive Scala Tests
# Generated: 2015-06-26T02:57:00

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "$DBVARIANT ScalaTest org.deepdive.test.unit.DataLoaderSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.DataLoaderSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.DataTypeParserSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.DataTypeParserSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.ExtractionManagerSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.ExtractionManagerSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.ExtractorRunnerSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.ExtractorRunnerSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.FactorFunctionParserSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.FactorFunctionParserSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.FactorWeightParserSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.FactorWeightParserSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.FileDataUtilsSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.FileDataUtilsSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.HelpersSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.HelpersSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.InferenceManagerSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.InferenceManagerSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.InputQueryParserSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.InputQueryParserSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.PostgresExtractionDataStoreSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.PostgresExtractionDataStoreSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.PostgresInferenceRunnerSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.PostgresInferenceRunnerSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.ProcessExecutorSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.ProcessExecutorSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.ProfilerSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.ProfilerSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.SamplerSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.SamplerSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.SettingsParserSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.SettingsParserSpec ; }
@test "$DBVARIANT ScalaTest org.deepdive.test.unit.TaskManagerSpec" { java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.TaskManagerSpec ; }
