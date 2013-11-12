package org.deepdive

import com.typesafe.config.ConfigFactory
import org.deepdive.context.ContextManager
import org.deepdive.inference.InferenceManager
import org.deepdive.context.Settings
import org.deepdive.extraction.{ExtractorExecutor, ExtractionTask}


/* DeepDive main entry point */
object Main extends Application {

  Pipeline.run(ConfigFactory.load)

}