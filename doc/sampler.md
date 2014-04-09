---
layout: default
---

# Dimmwitted High-Speed Sampler

This tutorial explains how to use DimmWitted, a high-speed sampler for DeepDive. DimmWitted binaries for both Mac OS X and Linux ship with DeepDive in the `util/` directory. These work out of the box on many modern systems, but setting up dependencies may be required on others (see below for more details)

### Using DimmWitted

In your `application.conf`, you can change the sampler executable as follows:
  
    sampler.sampler_cmd: "util/sampler-dw-mac gibbs"
    sampler.sampler_args: "-l 1000 -s 1 -i 1000 --alpha 0.01"

In the above, use `sampler-dw-mac` or `sampler-dw-linux` depending on which type of system your are on. Note that we have also changes the sampler parameters to use a larger number of iterations for learning and inference because each iteration takes much less time with the high-speed sampler.


### Setting up dependencies

We ship pre-built dependencies for Linux and Mac systems in the `lib/` folder. Extract them:

    cd lib
    unzip dw_mac.zip

Now, tell the executable to load the shared libraries by setting the appropriate environment variables (for example in your run.sh file). On Mac:
  
    export LD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_mac/lib/protobuf/lib:[DEEPDIVE_HOME]/lib/dw_mac/lib
    export DYLD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_mac

On Linux:
  
    export LD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_linux/lib:[DEEPDIVE_HOME]/lib/dw_linux/lib64
