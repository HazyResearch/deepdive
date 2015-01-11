DimmWitted
==========

# How fast is DimmWitted?

  - On Amazon EC2's FREE MACHINE (512M memory, 1 core). We can sample 3.6M varialbes/seconds.
  - On a 2-node Amazon EC2 machine, sampling 7 billion random variables, each of which has 10 features, takes 3 minutes. This means we can run inference for all living human beings on this planet with $15 (100 samples!)
  - On Macbook, DimmWitted runs 10x faster than DeepDive's default sampler.

# Pre-built Binary

We include a pre-built binary in the release folder for both Linux and Mac... 
Because we reply on lnuma, this is not always possible... But 
we successfully deployed this binary to the following configurations. Good luck!

  - Macbook or Apple Server
    - **(Mac OSX > 10.7 should work)**
    - Macbook Pro: Darwin MACHINENAME 13.0.0 Darwin Kernel Version 13.0.0: Thu Sep 19 22:22:27 PDT 2013; root:xnu-2422.1.72~6/RELEASE_X86_64 x86_64
    - Apple Server: Darwin MACHINENAME 10.8.0 Darwin Kernel Version 10.8.0: Tue Jun  7 16:33:36 PDT 2011; root:xnu-1504.15.3~1/RELEASE_I386 i386
  - Linux Servers:
    - **(Newer than 2006-released Linux kernel 2.6.18 should work)** 
    - Local Machine: Linux MACHINENAME 2.6.32-358.23.2.el6.x86_64 #1 SMP Sat Sep 14 05:32:37 EDT 2013 x86_64 x86_64 x86_64 GNU/Linux
    - Local Machine: Linux MACHINENAME 2.6.32-431.3.1.el6.x86_64 #1 SMP Fri Dec 13 06:58:20 EST 2013 x86_64 x86_64 x86_64 GNU/Linux
    - Local Machine: Linux MACHINENAME 2.6.32-431.3.1.el6.x86_64 #1 SMP Fri Dec 13 06:58:20 EST 2013 x86_64 x86_64 x86_64 GNU/Linux
    - Local Machine: Linux MACHINENAME 2.6.32-279.el6.x86_64 #1 SMP Fri Jun 22 12:19:21 UTC 2012 x86_64 x86_64 x86_64 GNU/Linux
    - Local Machine: Linux MACHINENAME 2.6.32-358.el6.x86_64 #1 SMP Fri Feb 22 00:31:26 UTC 2013 x86_64 x86_64 x86_64 GNU/Linux
    - Local Machine: Linux MACHINENAME 2.6.18-308.el5 #1 SMP Tue Feb 21 20:06:06 EST 2012 x86_64 x86_64 x86_64 GNU/Linux
    - Local Machine: Linux MACHINENAME 2.6.18-274.12.1.el5 #1 SMP Tue Nov 8 21:37:35 EST 2011 x86_64 x86_64 x86_64 GNU/Linux
  - EC2 Machines
    - **(The cheapest ones with Ubuntu12.04 worked!)**
    - EC2 Free Machine: Linux MACHINENAME 3.2.0-58-virtual #88-Ubuntu SMP Tue Dec 3 17:58:13 UTC 2013 x86_64 x86_64 x86_64 GNU/Linux
  
We haven't found machines that cannot work with this binary yet.

If you are lucky, the follow two commands will tell you whether it works or not

    tar xf dw.tar.bz2
    sh test.sh

For Mac, the filename is

    dw_mac.tar.bz2
    
instead. We assume at least MAC OSX 10.7.

# Installation

Sorry if you need to read this section to build DimmWitted by yourself. But fortunately, it is not that hard.

First, install dependencies

    make dep

Then

    make

This will use whatever in you $(CXX) variable to compile. We assume that you have > g++4.7.2 or clang++-4.2. To specify a compiler to use, type in something like

    CXX=/dfs/rulk/0/czhang/software/gcc/bin/g++ make

To test

    make gibbs

This should output things like

    #################MACHINE CONFIG#################
    # # NUMA Node        : 1
    # # Thread/NUMA Node : 4
    ################################################

    #################GIBBS SAMPLING#################
    # fg_file            : data3/graph.meta.csv
    # edge_file          : data3/graph.edges
    # weight_file        : data3/graph.weights
    # variable_file      : data3/graph.variables
    # factor_file        : data3/graph.factors
    # output_folder      : data3/
    # n_learning_epoch   : 100
    # n_samples/l. epoch : 10
    # n_inference_epoch  : 100
    # stepsize           : 0.01
    # decay              : 0.95
    ################################################
    # IGNORE -s (n_samples/l. epoch). ALWAYS -s 1. #
    # IGNORE -t (threads). ALWAYS USE ALL THREADS. #
    ################################################
    # nvar               : 75454
    # nfac               : 227278
    # nweight            : 30467
    # nedge              : 302732
    ################################################
    LOADED VARIABLES: #75454
             N_QUERY: #71509
             N_EVID : #3945
    LOADED FACTORS: #227278
    LOADED WEIGHTS: #30467
    LOADED EDGES: #302732
    FACTOR GRAPH: Safety Checking Passed...
    LEARNING EPOCH 0~1....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.01,lmax=2.1e+03,l2=2.1e+03
    LEARNING EPOCH 1~2....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0095,lmax=2.2e+03,l2=2.2e+03
    LEARNING EPOCH 2~3....0.0018 sec.,4.3e+07 vars/sec.,stepsize=0.009,lmax=2e+03,l2=2.1e+03
    LEARNING EPOCH 3~4....0.0016 sec.,4.6e+07 vars/sec.,stepsize=0.0086,lmax=1.8e+03,l2=1.8e+03
    LEARNING EPOCH 4~5....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0081,lmax=1.8e+03,l2=1.8e+03
    LEARNING EPOCH 5~6....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0077,lmax=1.7e+03,l2=1.7e+03
    LEARNING EPOCH 6~7....0.0037 sec.,2.1e+07 vars/sec.,stepsize=0.0074,lmax=1.8e+03,l2=1.8e+03
    LEARNING EPOCH 7~8....0.0031 sec.,2.5e+07 vars/sec.,stepsize=0.007,lmax=1.6e+03,l2=1.6e+03
    LEARNING EPOCH 8~9....0.0025 sec.,3.1e+07 vars/sec.,stepsize=0.0066,lmax=1.4e+03,l2=1.4e+03
    LEARNING EPOCH 9~10....0.0022 sec.,3.4e+07 vars/sec.,stepsize=0.0063,lmax=1.5e+03,l2=1.5e+03
    LEARNING EPOCH 10~11....0.0017 sec.,4.4e+07 vars/sec.,stepsize=0.006,lmax=1.5e+03,l2=1.5e+03
    LEARNING EPOCH 11~12....0.0018 sec.,4.3e+07 vars/sec.,stepsize=0.0057,lmax=1.5e+03,l2=1.5e+03
    LEARNING EPOCH 12~13....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0054,lmax=1.4e+03,l2=1.4e+03
    LEARNING EPOCH 13~14....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0051,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 14~15....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0049,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 15~16....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0046,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 16~17....0.0021 sec.,3.6e+07 vars/sec.,stepsize=0.0044,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 17~18....0.0035 sec.,2.2e+07 vars/sec.,stepsize=0.0042,lmax=1.4e+03,l2=1.4e+03
    LEARNING EPOCH 18~19....0.0025 sec.,3e+07 vars/sec.,stepsize=0.004,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 19~20....0.0032 sec.,2.3e+07 vars/sec.,stepsize=0.0038,lmax=1.4e+03,l2=1.4e+03
    LEARNING EPOCH 20~21....0.0033 sec.,2.3e+07 vars/sec.,stepsize=0.0036,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 21~22....0.0016 sec.,4.8e+07 vars/sec.,stepsize=0.0034,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 22~23....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0032,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 23~24....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0031,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 24~25....0.0016 sec.,4.6e+07 vars/sec.,stepsize=0.0029,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 25~26....0.0025 sec.,3.1e+07 vars/sec.,stepsize=0.0028,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 26~27....0.003 sec.,2.5e+07 vars/sec.,stepsize=0.0026,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 27~28....0.0048 sec.,1.6e+07 vars/sec.,stepsize=0.0025,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 28~29....0.003 sec.,2.6e+07 vars/sec.,stepsize=0.0024,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 29~30....0.0028 sec.,2.7e+07 vars/sec.,stepsize=0.0023,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 30~31....0.0028 sec.,2.7e+07 vars/sec.,stepsize=0.0021,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 31~32....0.0035 sec.,2.2e+07 vars/sec.,stepsize=0.002,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 32~33....0.0033 sec.,2.3e+07 vars/sec.,stepsize=0.0019,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 33~34....0.0019 sec.,4e+07 vars/sec.,stepsize=0.0018,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 34~35....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0017,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 35~36....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0017,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 36~37....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0016,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 37~38....0.0016 sec.,4.9e+07 vars/sec.,stepsize=0.0015,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 38~39....0.0017 sec.,4.6e+07 vars/sec.,stepsize=0.0014,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 39~40....0.0017 sec.,4.4e+07 vars/sec.,stepsize=0.0014,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 40~41....0.0019 sec.,4.1e+07 vars/sec.,stepsize=0.0013,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 41~42....0.0019 sec.,4e+07 vars/sec.,stepsize=0.0012,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 42~43....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.0012,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 43~44....0.0017 sec.,4.4e+07 vars/sec.,stepsize=0.0011,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 44~45....0.0018 sec.,4.1e+07 vars/sec.,stepsize=0.001,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 45~46....0.0028 sec.,2.7e+07 vars/sec.,stepsize=0.00099,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 46~47....0.003 sec.,2.5e+07 vars/sec.,stepsize=0.00094,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 47~48....0.0025 sec.,3e+07 vars/sec.,stepsize=0.0009,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 48~49....0.0024 sec.,3.2e+07 vars/sec.,stepsize=0.00085,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 49~50....0.0024 sec.,3.2e+07 vars/sec.,stepsize=0.00081,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 50~51....0.0022 sec.,3.4e+07 vars/sec.,stepsize=0.00077,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 51~52....0.0018 sec.,4.3e+07 vars/sec.,stepsize=0.00073,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 52~53....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00069,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 53~54....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00066,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 54~55....0.0015 sec.,5e+07 vars/sec.,stepsize=0.00063,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 55~56....0.0015 sec.,5.1e+07 vars/sec.,stepsize=0.0006,lmax=1e+03,l2=1e+03
    LEARNING EPOCH 56~57....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00057,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 57~58....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00054,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 58~59....0.0018 sec.,4.3e+07 vars/sec.,stepsize=0.00051,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 59~60....0.0018 sec.,4.1e+07 vars/sec.,stepsize=0.00048,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 60~61....0.0026 sec.,2.9e+07 vars/sec.,stepsize=0.00046,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 61~62....0.0025 sec.,3e+07 vars/sec.,stepsize=0.00044,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 62~63....0.0025 sec.,3e+07 vars/sec.,stepsize=0.00042,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 63~64....0.0026 sec.,2.9e+07 vars/sec.,stepsize=0.00039,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 64~65....0.0036 sec.,2.1e+07 vars/sec.,stepsize=0.00038,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 65~66....0.0018 sec.,4.3e+07 vars/sec.,stepsize=0.00036,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 66~67....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00034,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 67~68....0.003 sec.,2.5e+07 vars/sec.,stepsize=0.00032,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 68~69....0.0028 sec.,2.7e+07 vars/sec.,stepsize=0.00031,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 69~70....0.0024 sec.,3.1e+07 vars/sec.,stepsize=0.00029,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 70~71....0.0025 sec.,3e+07 vars/sec.,stepsize=0.00028,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 71~72....0.0025 sec.,3e+07 vars/sec.,stepsize=0.00026,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 72~73....0.0017 sec.,4.4e+07 vars/sec.,stepsize=0.00025,lmax=1.1e+03,l2=1.2e+03
    LEARNING EPOCH 73~74....0.0025 sec.,3e+07 vars/sec.,stepsize=0.00024,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 74~75....0.0034 sec.,2.2e+07 vars/sec.,stepsize=0.00022,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 75~76....0.0033 sec.,2.3e+07 vars/sec.,stepsize=0.00021,lmax=1.4e+03,l2=1.4e+03
    LEARNING EPOCH 76~77....0.0047 sec.,1.6e+07 vars/sec.,stepsize=0.0002,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 77~78....0.003 sec.,2.5e+07 vars/sec.,stepsize=0.00019,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 78~79....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00018,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 79~80....0.0018 sec.,4.1e+07 vars/sec.,stepsize=0.00017,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 80~81....0.0019 sec.,4.1e+07 vars/sec.,stepsize=0.00017,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 81~82....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00016,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 82~83....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00015,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 83~84....0.0015 sec.,4.9e+07 vars/sec.,stepsize=0.00014,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 84~85....0.0019 sec.,3.9e+07 vars/sec.,stepsize=0.00013,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 85~86....0.0018 sec.,4.1e+07 vars/sec.,stepsize=0.00013,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 86~87....0.0018 sec.,4.2e+07 vars/sec.,stepsize=0.00012,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 87~88....0.0046 sec.,1.7e+07 vars/sec.,stepsize=0.00012,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 88~89....0.0036 sec.,2.1e+07 vars/sec.,stepsize=0.00011,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 89~90....0.0037 sec.,2e+07 vars/sec.,stepsize=0.0001,lmax=1.3e+03,l2=1.3e+03
    LEARNING EPOCH 90~91....0.006 sec.,1.3e+07 vars/sec.,stepsize=9.9e-05,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 91~92....0.0049 sec.,1.5e+07 vars/sec.,stepsize=9.4e-05,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 92~93....0.0018 sec.,4.2e+07 vars/sec.,stepsize=8.9e-05,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 93~94....0.0019 sec.,4.1e+07 vars/sec.,stepsize=8.5e-05,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 94~95....0.0019 sec.,4.1e+07 vars/sec.,stepsize=8.1e-05,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 95~96....0.0017 sec.,4.5e+07 vars/sec.,stepsize=7.7e-05,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 96~97....0.0018 sec.,4.2e+07 vars/sec.,stepsize=7.3e-05,lmax=1.2e+03,l2=1.2e+03
    LEARNING EPOCH 97~98....0.0019 sec.,4e+07 vars/sec.,stepsize=6.9e-05,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 98~99....0.0029 sec.,2.6e+07 vars/sec.,stepsize=6.6e-05,lmax=1.1e+03,l2=1.1e+03
    LEARNING EPOCH 99~100....0.0032 sec.,2.4e+07 vars/sec.,stepsize=6.2e-05,lmax=1.1e+03,l2=1.1e+03
    TOTAL LEARNING TIME: 0.24 sec.
    LEARNING SNIPPETS (QUERY WEIGHTS):
       0 -3e+02
       1 -0.36
       2 0
       3 -0.74
       4 0
       5 0
       6 0
       7 0
       8 0
       9 0
       ...
    DUMPING... PROTOCOL: data3//inference_result.out.weights
    DUMPING... TEXT    : data3//inference_result.out.weights.text
    INFERENCE EPOCH 0~1....0.0078 sec.,9.6e+06 vars/sec
    INFERENCE EPOCH 1~2....0.0066 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 2~3....0.0084 sec.,9e+06 vars/sec
    INFERENCE EPOCH 3~4....0.007 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 4~5....0.0082 sec.,9.2e+06 vars/sec
    INFERENCE EPOCH 5~6....0.0099 sec.,7.6e+06 vars/sec
    INFERENCE EPOCH 6~7....0.0096 sec.,7.9e+06 vars/sec
    INFERENCE EPOCH 7~8....0.012 sec.,6.3e+06 vars/sec
    INFERENCE EPOCH 8~9....0.007 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 9~10....0.0098 sec.,7.7e+06 vars/sec
    INFERENCE EPOCH 10~11....0.0085 sec.,8.8e+06 vars/sec
    INFERENCE EPOCH 11~12....0.01 sec.,7.5e+06 vars/sec
    INFERENCE EPOCH 12~13....0.0093 sec.,8.1e+06 vars/sec
    INFERENCE EPOCH 13~14....0.0069 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 14~15....0.0094 sec.,8e+06 vars/sec
    INFERENCE EPOCH 15~16....0.012 sec.,6.5e+06 vars/sec
    INFERENCE EPOCH 16~17....0.0088 sec.,8.6e+06 vars/sec
    INFERENCE EPOCH 17~18....0.0097 sec.,7.8e+06 vars/sec
    INFERENCE EPOCH 18~19....0.0074 sec.,1e+07 vars/sec
    INFERENCE EPOCH 19~20....0.0078 sec.,9.7e+06 vars/sec
    INFERENCE EPOCH 20~21....0.01 sec.,7.2e+06 vars/sec
    INFERENCE EPOCH 21~22....0.0069 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 22~23....0.0082 sec.,9.2e+06 vars/sec
    INFERENCE EPOCH 23~24....0.01 sec.,7.5e+06 vars/sec
    INFERENCE EPOCH 24~25....0.0068 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 25~26....0.011 sec.,6.9e+06 vars/sec
    INFERENCE EPOCH 26~27....0.012 sec.,6.3e+06 vars/sec
    INFERENCE EPOCH 27~28....0.0091 sec.,8.3e+06 vars/sec
    INFERENCE EPOCH 28~29....0.007 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 29~30....0.0071 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 30~31....0.0095 sec.,7.9e+06 vars/sec
    INFERENCE EPOCH 31~32....0.0083 sec.,9.1e+06 vars/sec
    INFERENCE EPOCH 32~33....0.0095 sec.,7.9e+06 vars/sec
    INFERENCE EPOCH 33~34....0.009 sec.,8.4e+06 vars/sec
    INFERENCE EPOCH 34~35....0.0069 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 35~36....0.012 sec.,6.3e+06 vars/sec
    INFERENCE EPOCH 36~37....0.0092 sec.,8.2e+06 vars/sec
    INFERENCE EPOCH 37~38....0.01 sec.,7.5e+06 vars/sec
    INFERENCE EPOCH 38~39....0.011 sec.,7.1e+06 vars/sec
    INFERENCE EPOCH 39~40....0.0069 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 40~41....0.01 sec.,7.4e+06 vars/sec
    INFERENCE EPOCH 41~42....0.0082 sec.,9.2e+06 vars/sec
    INFERENCE EPOCH 42~43....0.0085 sec.,8.8e+06 vars/sec
    INFERENCE EPOCH 43~44....0.01 sec.,7.3e+06 vars/sec
    INFERENCE EPOCH 44~45....0.0073 sec.,1e+07 vars/sec
    INFERENCE EPOCH 45~46....0.0097 sec.,7.8e+06 vars/sec
    INFERENCE EPOCH 46~47....0.009 sec.,8.4e+06 vars/sec
    INFERENCE EPOCH 47~48....0.007 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 48~49....0.0091 sec.,8.3e+06 vars/sec
    INFERENCE EPOCH 49~50....0.0096 sec.,7.9e+06 vars/sec
    INFERENCE EPOCH 50~51....0.012 sec.,6.2e+06 vars/sec
    INFERENCE EPOCH 51~52....0.0081 sec.,9.3e+06 vars/sec
    INFERENCE EPOCH 52~53....0.0088 sec.,8.6e+06 vars/sec
    INFERENCE EPOCH 53~54....0.01 sec.,7.2e+06 vars/sec
    INFERENCE EPOCH 54~55....0.007 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 55~56....0.0099 sec.,7.6e+06 vars/sec
    INFERENCE EPOCH 56~57....0.0077 sec.,9.8e+06 vars/sec
    INFERENCE EPOCH 57~58....0.0095 sec.,7.9e+06 vars/sec
    INFERENCE EPOCH 58~59....0.01 sec.,7.5e+06 vars/sec
    INFERENCE EPOCH 59~60....0.0069 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 60~61....0.012 sec.,6.1e+06 vars/sec
    INFERENCE EPOCH 61~62....0.0082 sec.,9.2e+06 vars/sec
    INFERENCE EPOCH 62~63....0.0077 sec.,9.9e+06 vars/sec
    INFERENCE EPOCH 63~64....0.01 sec.,7.4e+06 vars/sec
    INFERENCE EPOCH 64~65....0.0084 sec.,9e+06 vars/sec
    INFERENCE EPOCH 65~66....0.0092 sec.,8.2e+06 vars/sec
    INFERENCE EPOCH 66~67....0.0088 sec.,8.6e+06 vars/sec
    INFERENCE EPOCH 67~68....0.0064 sec.,1.2e+07 vars/sec
    INFERENCE EPOCH 68~69....0.0074 sec.,1e+07 vars/sec
    INFERENCE EPOCH 69~70....0.009 sec.,8.3e+06 vars/sec
    INFERENCE EPOCH 70~71....0.014 sec.,5.5e+06 vars/sec
    INFERENCE EPOCH 71~72....0.009 sec.,8.4e+06 vars/sec
    INFERENCE EPOCH 72~73....0.0092 sec.,8.2e+06 vars/sec
    INFERENCE EPOCH 73~74....0.011 sec.,7.1e+06 vars/sec
    INFERENCE EPOCH 74~75....0.0061 sec.,1.2e+07 vars/sec
    INFERENCE EPOCH 75~76....0.0091 sec.,8.3e+06 vars/sec
    INFERENCE EPOCH 76~77....0.0089 sec.,8.5e+06 vars/sec
    INFERENCE EPOCH 77~78....0.0094 sec.,8e+06 vars/sec
    INFERENCE EPOCH 78~79....0.01 sec.,7.3e+06 vars/sec
    INFERENCE EPOCH 79~80....0.007 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 80~81....0.012 sec.,6.4e+06 vars/sec
    INFERENCE EPOCH 81~82....0.0096 sec.,7.9e+06 vars/sec
    INFERENCE EPOCH 82~83....0.0072 sec.,1e+07 vars/sec
    INFERENCE EPOCH 83~84....0.0098 sec.,7.7e+06 vars/sec
    INFERENCE EPOCH 84~85....0.0091 sec.,8.3e+06 vars/sec
    INFERENCE EPOCH 85~86....0.0093 sec.,8.1e+06 vars/sec
    INFERENCE EPOCH 86~87....0.011 sec.,6.9e+06 vars/sec
    INFERENCE EPOCH 87~88....0.008 sec.,9.4e+06 vars/sec
    INFERENCE EPOCH 88~89....0.011 sec.,7.1e+06 vars/sec
    INFERENCE EPOCH 89~90....0.012 sec.,6.4e+06 vars/sec
    INFERENCE EPOCH 90~91....0.01 sec.,7.3e+06 vars/sec
    INFERENCE EPOCH 91~92....0.008 sec.,9.5e+06 vars/sec
    INFERENCE EPOCH 92~93....0.007 sec.,1.1e+07 vars/sec
    INFERENCE EPOCH 93~94....0.0083 sec.,9.1e+06 vars/sec
    INFERENCE EPOCH 94~95....0.0093 sec.,8.1e+06 vars/sec
    INFERENCE EPOCH 95~96....0.0097 sec.,7.8e+06 vars/sec
    INFERENCE EPOCH 96~97....0.009 sec.,8.4e+06 vars/sec
    INFERENCE EPOCH 97~98....0.0092 sec.,8.2e+06 vars/sec
    INFERENCE EPOCH 98~99....0.012 sec.,6.4e+06 vars/sec
    INFERENCE EPOCH 99~100....0.0062 sec.,1.2e+07 vars/sec
    TOTAL INFERENCE TIME: 0.91 sec.
    INFERENCE SNIPPETS (QUERY VARIABLES):
       0 EXP=0.33  NSAMPLE=1e+02
       2 EXP=0.33  NSAMPLE=1e+02
       4 EXP=0.33  NSAMPLE=1e+02
       5 EXP=0.3  NSAMPLE=1e+02
       7 EXP=0.25  NSAMPLE=1e+02
       9 EXP=0.49  NSAMPLE=1e+02
       14 EXP=0.25  NSAMPLE=1e+02
       15 EXP=0.1  NSAMPLE=1e+02
       16 EXP=0.95  NSAMPLE=1e+02
       17 EXP=0.14  NSAMPLE=1e+02
       ...
    DUMPING... PROTOCOL: data3//inference_result.out
    DUMPING... TEXT    : data3//inference_result.out.text
    INFERENCE CALIBRATION (QUERY BINS):
    PROB BIN 0.0~0.1  -->  # 474
    PROB BIN 0.1~0.2  -->  # 7978
    PROB BIN 0.2~0.3  -->  # 16606
    PROB BIN 0.3~0.4  -->  # 17825
    PROB BIN 0.4~0.5  -->  # 13871
    PROB BIN 0.5~0.6  -->  # 6765
    PROB BIN 0.6~0.7  -->  # 3207
    PROB BIN 0.7~0.8  -->  # 1535
    PROB BIN 0.8~0.9  -->  # 104
    PROB BIN 0.9~0.10  -->  # 3144
