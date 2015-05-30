###################################################
# PLEASE EDIT THIS SECTION TO FIT YOUR MACHINE SPEC,
# ESPECIALLY DATA_DIR, NUM_DATA_NODES, and DATA_NODE_SHARED_BUFFERS.
KERNAL_SHMMAX=429496729600  #400GB
CC_USER=ccuser
###################################################


###################################################
# Typically you can leave this section alone
TARGET_DIR=/opt/pgxl
###################################################

sudo apt-get update
sudo apt-get -y install -y screen curl git rsync openssl locales openssh-server openssh-client
sudo apt-get -y install -y gcc flex bison make cmake jade openjade docbook docbook-dsssl
sudo apt-get -y install zlib1g-dev libreadline6-dev python-dev libssl-dev
sudo localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

sudo mkdir -p $TARGET_DIR
sudo chown $CC_USER:$CC_USER $TARGET_DIR

echo "/usr/local/lib" | sudo tee -a /etc/ld.so.conf
echo "$TARGET_DIR/lib" | sudo tee -a /etc/ld.so.conf

echo "kernel.shmmax = $KERNAL_SHMMAX" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p /etc/sysctl.conf

echo "   StrictHostKeyChecking no" | sudo tee -a /etc/ssh/ssh_config
sudo service ssh restart

# must add newline here
echo "
MaxStartups 100" | sudo tee -a /etc/ssh/sshd_config
sudo service ssh restart
echo '
kernel.sem = 1000  32000  32  1000' | sudo tee -a /etc/sysctl.conf
sudo sysctl -w kernel.sem="1000  32000  32  1000"

