---
layout: default
title: High-speed Sampler
---

# The DimmWitted high-speed sampler

This document briefly presents [DimmWitted](https://github.com/HazyResearch/sampler), a high-speed [Gibbs sampler](inference.md#gibbs) for DeepDive.

In `deepdive.conf`, you can swap the default sampler executable with something else follows:

```hocon
deepdive.sampler.sampler_cmd: "/path/to/your/sampler gibbs"
```

### Sampler arguments

The sampler executable can be invoked independently of DeepDive. The following
arguments to the sampler executable can be used:

    -q, --quiet
        Quiet output

    -c <int>,  --n_datacopy <int> (Linux only)
        Number of data copies.  One or more NUMA nodes can hold a copy of the
        factor graph and their CPU cores run the threads.  This argument
        specifies how many partitions the NUMA nodes should be grouped into.
        Default is to keep a copy of the factor graph in every NUMA node.

    -t <int>,  --n_threads <int>
        Number of threads to use.  Defaults to zero (0) which uses all
        available threads.  The number of threads are equally divided and
        assigned to each data copy when --n_datacopy is greater than 1.

    -w <weightsFile> | --weights <weightsFile>
        Weights file (required)
        It is a binary format file output by DeepDive.

    -v <variablesFile> | --variables <variablesFile>
        Variables file (required)
        It is a binary format file output by DeepDive.

    --domains <domainsFile>
        Categorical variable domains file (optional)
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

