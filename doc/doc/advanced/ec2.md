---
layout: default
---

# Using DeepDive on EC2

Although we no longer distribute a pre-configured [Amazon EC2 Machine
Image](http://aws.amazon.com/ec2/) (AMI), DeepDive can be installed fairly quickly on an EC2 instance.

### Launching an Instance

The following are the steps needed to launch an EC2 instance and start DeepDive in it with Postgres:

- Choose the "US-East" region (top right menu)
- Click on "Launch Instance", choose "Quick Start" and "Select" the "Ubuntu" AMI image.
- In the next step, pick the instance type. For example, the `m3.large` instance type is fine
  for testing-purposes. We recommend using the compute- or memory-optimized
  instances (depending on your use case) for production purposes.
- Follow the wizard to launch the instance.
- SSH into the instance as `ubuntu` with the private key you specified in the
  AWS wizard.
- Run the following command to install Postgres and DeepDive.

    ```bash
    curl -fsSL deepdive.stanford.edu/install | bash -s postgres deepdive
    ```

    You will be asked to create a new password for Postgres user account.
    Remember to set `PGPASSWORD` environment varible to that password, e.g., `pa$$w0rd`, to let DeepDive and `psql` command access the database.

    ```bash
    export PGPASSWORD='pa$$w0rd'
    ```

- Navigate to `./deepdive` and run tests to confirm that the
  installation was successful.

    ```bash
    cd ./deepdive
    make test
    ```

### Notes

- For improved I/O performance the postgresql data directory is created on the
  Amazon instance storage in `/media/ephemeral0`. This means that if you shut
  down your instance, **all postgresql data will be lost**. Make sure to backup
  your data or store in on the EBS volume if you want to keep it.

