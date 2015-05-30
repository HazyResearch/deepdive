#!/usr/bin/env bash
# DeepDive installers for Ubuntu Linux

LSB=$(lsb_release -r 2>/dev/null) 
case ${LSB##*	} in
    12.04|14.04|15.04) true ;;
    *) error "Ubuntu $LSB found: This installer only works with Ubuntu 12.04, 14.04, and 15.04."
esac

install_deepdive_build_deps() {
    set -x
    sudo apt-get update
    sudo apt-get install -y git unzip
}

install_deepdive_runtime_deps() {
    set -x
    # Installation of the dependencies for the DeepDive stack.
    # (deepdive, sampler, mindbender)

    sudo apt-get update

    # Many dependencies are already available in TravisCI
    if [ -z "${TRAVIS:-}" ]; then
        # automate license agreement for oracle java
        echo debconf shared/accepted-oracle-license-v1-1 select true |
        sudo debconf-set-selections
        echo debconf shared/accepted-oracle-license-v1-1 seen true |
        sudo debconf-set-selections

        # install oracle java
        sudo add-apt-repository -y ppa:webupd8team/java
        sudo apt-get update
        sudo apt-get install -y oracle-java8-installer 

        # install dependencies for deepdive
        sudo apt-get install -y python-software-properties make
    fi

    # install additional packages for deepdive
    sudo apt-get install -y gnuplot bc

    # install additional packages for sampler
    #sudo apt-get install -y g++ cmake libnuma-dev libtclap-dev

    # install additional packages for mindbender
    #sudo apt-get install -y libyaml-dev python-pip

    # install performance analysis tools
    #sudo apt-get install -y sysstat
}

install_postgres_xl() {
    source_os_script pgxl
}

install_postgres() {
    set -x
    sudo apt-get install -y postgresql
    sudo -u postgres createuser --superuser --no-password $USER || true
}
