#!/usr/bin/env bash
# DeepDive installers for Debian/Ubuntu Linux

LSB=$(lsb_release -ir | cut -f2)
case ${LSB} in
    Debian*8*|Debian*7*) true ;;
    Ubuntu*12.04|Ubuntu*14.04|Ubuntu*15.04) true ;;
    *) error "$LSB found: This installer may not work on your OS." ||
        error "It has been tested only on Debian 7 and 8, Ubuntu 12.04, 14.04, and 15.04." ||
        true  # don't fail here as it might work for other versions
esac

install__deepdive_build_deps() {
    set -x
    sudo apt-get update
    sudo apt-get install -y git bzip2 unzip make default-jdk

    # sampler build deps
    #sudo apt-get install -y g++ cmake libnuma-dev libtclap-dev

    # mindbender build deps
    #sudo apt-get install -y libyaml-dev python-pip
}

install__deepdive_runtime_deps() {
    set -x
    # install all runtime dependencies for DeepDive
    sudo apt-get update

    # Many dependencies are already available in TravisCI
    if [ -z "${TRAVIS:-}" ]; then
        # install dependencies for deepdive
        sudo apt-get install -y python-software-properties default-jre-headless
    fi

    # install additional packages for deepdive
    sudo apt-get install -y gnuplot bc
}

install_postgres_xl() {
    source_os_script pgxl
}

install_postgres() {
    set -x
    sudo apt-get update
    sudo apt-get install -y postgresql
    sudo apt-get install -y postgresql-plpython-`ls -1 /var/lib/postgresql/ | head -n 1`
    if [ -z "${TRAVIS:-}" ]; then
        sudo -u postgres dropuser --if-exists $USER
        sudo -u postgres createuser --superuser --pwprompt $USER || true
    fi
}
