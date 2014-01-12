package org.deepdive.settings

/* A DeepDive pipeline that specifies which extractors and factor tasks should be executed */
case class Pipeline(id: String, tasks: Set[String])

/* User settings pipelines */
case class PipelineSettings(activePipelineName: Option[String], pipelines: List[Pipeline]) {
  def activePipeline : Option[Pipeline] = activePipelineName.flatMap { name =>
    pipelines.find(_.id == name)
  }
}