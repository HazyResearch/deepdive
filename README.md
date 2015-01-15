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
