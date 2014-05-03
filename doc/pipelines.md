---
layout: default
---

# Pipelines

While testing extractors and inference rules it can be useful to execute only a subset of them. Deepdive allows you define *pipelines* that define which extractors and inference rules are "active". 

### Adding Pipelines

You can define custom pipelines by adding an optional configuration setting:

    deepdive {
      pipeline {
        run: myPipeline
        pipelines {
          myPipeline: [
            extractor1
            extractor2
            inferenceRule1
            ]
        }
      }
    }

Each pipeline is defined as an array of tasks, where the names of the tasks are the names of your extractor or inference rules. **When no pipelines are specified, DeepDive executes all extractors and inference rules.**
