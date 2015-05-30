Preparing a cloud instance for DeepDive
=======================================

1. Launch an instance on Azure or EC2 with the following characteristics
   * at least 8 or 16 cores and 112 GB memory for processing millions of documents
   * Ubuntu 15.04

2. After launching the instance attach an extra disk, with say 500GB capacity.

3. Transfer the scripts to the instance. 

4. As a user with root permissions, execute the following scripts (in order):
   ```
   ./root0_attach.sh
   ./root1_user.sh   # <--- requires user input
   ./root2_pgxl.sh
   ./root3_pg.sh
   ./root4_dd.sh
   ```
5. Done. All other dependencies can be installed without sudo.
