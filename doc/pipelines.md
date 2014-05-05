---
layout: default
---

# Pipelines

While testing extractors and inference rules it can be useful to execute only a subset of them. DeepDive allows you define *pipelines* that define which extractors and inference rules are "active". 

### Adding Pipelines

You can define custom pipelines by adding an optional configuration setting:

    deepdive {
      pipeline.run: myPipeline
      pipeline.pipelines {
        myPipeline: [
            extractor1
            extractor2
            inferenceRule1
          ]
      }
    }

Each pipeline is defined as an array of tasks, where the names of the tasks are the names of your extractor or inference rules. 

When no pipelines are specified, DeepDive executes all extractors and inference rules.

When no inference factors are active in the pipeline, DeepDive will only perform extractors, while skipping learning and inference.

Developers can set `pipeline.relearn_from` to an output directory of one execution of DeepDive, to use an existing grounded factor graph for learning and inference. In this case DeepDive would skip all extractors and grounding. This would be useful for tuning sampler parameters:

    deepdive {
      pipeline.relearn_from: "/PATH/TO/DEEPDIVE/HOME/out/2014-05-02T131658/"
    }
