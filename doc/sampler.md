---
layout: default
title: High-speed Sampler
---

# The DimmWitted high-speed sampler

This document briefly presents DimmWitted, a high-speed [Gibbs
sampler](inference.md#gibbs) for DeepDive.


In `deepdive.conf`, you can change the sampler executable as follows:

```bash
deepdive {
  sampler.sampler_cmd: "util/sampler-dw-mac gibbs"
}
```

Use `sampler-dw-mac` or `sampler-dw-linux` depending on which type
of system your are on.

Since [version 0.03](changelog/0.03-alpha.md), DeepDive automatically
chooses the correct executable based on the system environment, so we recommend to
omit the `sampler_cmd` directive.

### Sampler arguments

The sampler executable can be invoked independently of DeepDive. The following
arguments to the sampler executable can be used:

    -q, --quiet
        Quiet output

    -c <int>,  --n_datacopy <int> (Linux only)
        Number of data copies. Each NUMA node has a copy of factor graph. This
        argument specifies number of NUMA nodes to use. Default is using all
        NUMA nodes.

    -w <weightsFile> | --weights <weightsFile>
        Weights file (required)
        It is a binary format file output by DeepDive.

    -v <variablesFile> | --variables <variablesFile>
        Variables file (required)
        It is a binary format file output by DeepDive.

    -f <factorsFile> | --factors <factorsFile>
        Factors file (required)
        It is a binary format file output by DeepDive.

    -m <metaFile> | --fg_meta <metaFile>
        Factor graph meta data file file (required)
        It is a text file containing factor graph meta information
        as well as paths to weight/variable/factor/edge files.

    -o <outputFile> | --outputFile <outputFile>
        Output file path (required)

    -i <numSamplesInference> | --n_inference_epoch <numSamplesInference>
        Number of iterations (epochs) during inference (required)

    -l <learningNumIterations> | --n_learning_epoch <learningNumIterations>
        Number of iterations (epochs) during weight learning (required)

    -s <learningNumSamples> | --n_samples_per_learning_epoch <learningNumSamples>
        Number of samples per iteration during weight learning (required)

    -a <learningRate> | --alpha <learningRate> | --stepsize <learningRate>
        The learning rate for gradient descent (default: 0.1)

    -d <diminishRate> | --diminish <diminishRate>
        The diminish rate for learning (default: 0.95).
        Learning rate will shrink by this parameter after each iteration.

    -b <regularizationParameter> | --reg_param <regularizationParameter>
        The l2 regularization parameter for learning (default: 0.01).

    --sample_evidence
        Output probablities for evidence variables. Default is off, i.e., output
        only contains probabilities for non-evidence variables.

    --learn_non_evidence
        Sample non-evidence variables during learning. Default if off. This option
        should be turned on if there exists a factor connecting evidence and non-evidence
        variables.

You can see a detailed list by running `deepdive env sampler-dw gibbs --help`.

