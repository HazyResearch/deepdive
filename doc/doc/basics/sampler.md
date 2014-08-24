---
layout: default
---

# The DimmWitted high-speed sampler

This document briefly presents DimmWitted, a high-speed [Gibbs
sampler](../general/inference.html#gibbs) for DeepDive.


In `application.conf`, you can change the sampler executable as follows:
  
```bash
deepdive {
  sampler.sampler_cmd: "util/sampler-dw-mac gibbs"
  sampler.sampler_args: "-l 1000 -s 1 -i 1000 --alpha 0.01"
}
```

Use `sampler-dw-mac` or `sampler-dw-linux` depending on which type
of system your are on. Note that we have also changed the sampler parameters to
use a larger number of iterations for learning and inference because each
iteration takes much less time with the high-speed sampler.

Since [version 0.03](../changelog/0.03-alpha.html), DeepDive automatically
chooses the correct executable based on the system environment, so we recommend to
omit the `sampler_cmd` directive.

### Sampler arguments

The sampler executable can be invoked independently of DeepDive. The following
arguments to the sampler executable are used to specify input files, output
file, and learning and inference parameters:


    -w <weightsFile> | --weights <weightsFile>
        weights file (required)
        It is a binary format file output by DeepDive.

    -v <variablesFile> | --variables <variablesFile>
        variables file (required)
        It is a binary format file output by DeepDive.

    -f <factorsFile> | --factors <factorsFile>
        factors file (required)
        It is a binary format file output by DeepDive.

    -e <edgesFile> | --edges <edgesFile>
        edges file (required)
        It is a binary format file output by DeepDive.

    -m <metaFile> | --fg_meta <metaFile>
        factor graph meta data file file (required)
        It is a text file containing factor graph meta information
            as well as paths to weight/variable/factor/edge files.

    -o <outputFile> | --outputFile <outputFile>
        output file path (required)

    -i <numSamplesInference> | --n_inference_epoch <numSamplesInference>
        number of iterations (epochs) during inference (required)

    -l <learningNumIterations> | --n_learning_epoch <learningNumIterations>
        number of iterations (epochs) during weight learning (required)

    -s <learningNumSamples> | --n_samples_per_learning_epoch <learningNumSamples>
        number of samples per iteration during weight learning (required)

    -a <learningRate> | --alpha <learningRate> | --stepsize <learningRate>
        the learning rate for gradient descent (default: 0.01)

    -d <diminishRate> | --diminish <diminishRate>
        the diminish rate for learning (default: 0.95).
        Learning rate will shrink by this parameter after each iteration.

<!-- TODO Feiran: proofreading these -->

You can see a detailed list by running `util/sampler-dw-mac gibbs --help` or `util/sampler-dw-linux gibbs --help`.

