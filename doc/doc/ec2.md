---
layout: default
---

# Using DeepDive on EC2

We provide pre-configured [Amazon EC2](http://aws.amazon.com/ec2/) AMI that comes with DeepDive. The AMI uses the Amazon Linux distribution.

### Using the AMI

In the Amazon EC2 console you can launch the AMI as follows:

- Choose the "US-East" region (top right menu)
- Click on "Launch Instance", choose "Community AMIs" and search for "DeepDive" (AMI id: ami-8a5fbce2). Choose the AMI image.
- In the next step, pick the instance type. The `m1.large` instance type is fine for testing-purposes. We recommend using the compute- or memory-optimized instances (depending on your use case) for production purposes.
- Follow the wizard to launch the instance.
- SSH into the instance as `ec2-user` with the private key set you specified in the AWS wizard.
- Execute `~/init.sh` to start postgresql and update DeepDive.
- Navigate to `~/deepdive` and execute `./test.sh` to make sure the installation was successful.


### Notes

- For improved I/O performance the postgresql data directory is created on the Amazon instance store in `/media/ephemeral0`. This means that if you shut down your instance, all postgresql data will be lost. Make sure to backup your data or store in on the EBS volume if you want to keep it.
