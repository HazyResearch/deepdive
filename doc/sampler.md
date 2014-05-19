---
layout: default
---

# DimmWitted High-Speed Sampler

This tutorial explains how to use DimmWitted, a high-speed sampler for DeepDive. Note DimmWitted only supports 64-bit systems. DimmWitted binaries for both Mac OS X and Linux ship with DeepDive in the `util/` directory. These work out of the box on many modern systems, but setting up dependencies may be required on others (see below for more details).


### Setting up dependencies

We ship pre-built dependencies for Linux and Mac systems in the `lib/` folder. Extract them:

    cd lib
    unzip dw_mac.zip

Now, tell the executable to load the shared libraries by setting the appropriate environment variables (for example in your run.sh file). On Mac:
  
    export LD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_mac/lib/protobuf/lib:[DEEPDIVE_HOME]/lib/dw_mac/lib
    export DYLD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_mac

On Linux:
  
    export LD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_linux/lib:[DEEPDIVE_HOME]/lib/dw_linux/lib64
    

### Using DimmWitted

In your `application.conf`, you can change the sampler executable as follows:
  
    deepdive {
      sampler.sampler_cmd: "util/sampler-dw-mac gibbs"
      sampler.sampler_args: "-l 1000 -s 1 -i 1000 --alpha 0.01"
    }

In the above, use `sampler-dw-mac` or `sampler-dw-linux` depending on which type of system your are on. Note that we have also changed the sampler parameters to use a larger number of iterations for learning and inference because each iteration takes much less time with the high-speed sampler.

In the current system since [version 0.03](changelog/0.03-alpha.html), DeepDive will automatically choose mac / linux sampler based on your environment, so sampler_cmd is recommended to be omitted.

    
### Sampler arguments

The sampler executable file can be invoked independently of DeepDive. The following arguments to the sampler executable are used to specify input files, output file, and learning and inference parameters:

    -w <weightsFile> | --weights <weightsFile>
        weights file (required)
    -v <variablesFile> | --variables <variablesFile>
        variables file (required)
    -f <factorsFile> | --factors <factorsFile>
        factors file (required)
    -e <edgesFile> | --edges <edgesFile>
        edges file (required)
    -m <metaFile> | --fg_meta <metaFile>
        factor graph meta data file file (required)
    -o <outputFile> | --outputFile <outputFile>
        output file path (required)
    -i <numSamplesInference> | --n_inference_epoch <numSamplesInference>
        number of samples during inference (required)
    -l <learningNumIterations> | --n_learning_epoch <learningNumIterations>
        number of iterations during weight learning (required)
    -s <learningNumSamples> | --n_samples_per_learning_epoch <learningNumSamples>
        number of samples per iteration during weight learning (required)
    -a <learningRate> | --alpha <learningRate> | --stepsize <learningRate>
        the learning rate for gradient descent (default: 0.01)
    -d <diminishRate> | --diminish <diminishRate>
        the diminish rate for learning (default: 0.95)


