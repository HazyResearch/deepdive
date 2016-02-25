#!/usr/bin/env bash
# DeepDive installers for RedHat-like Linux

redHatRelease=$(cat /etc/redhat-release)
case $redHatRelease in
    "Red Hat Enterprise Linux"*) true ;;
    "CentOS"*) true ;;
    "Fedora"*) true ;;
    *) error "This installer may not work on your OS: $redHatRelease" ||
        error "It has been tested only on Red Hat Enterprise Linux 7, CentOS, and Fedora." ||
        true  # don't fail here as it might work for other versions
esac

install__deepdive_build_deps() {
    set -x
    sudo yum groupinstall -y 'Development Tools'
    build_deps=(
        git
        rsync
        bzip2
        bzip2-devel
        #xz-utils
        flex
        java
        perl
        python
        bc
        # mindbender
        ed
        # sampler
        #gcc-4.8
        numactl-devel
        cmake
        unzip
    )
    sudo yum install -y "${build_deps[@]}"
}

install__deepdive_runtime_deps() {
    set -x
    # install all runtime dependencies for DeepDive
    runtime_deps=(
        perl
        java
        python
        gnuplot
        bc
        # for graphviz
        libtool-ltdl
    )
    sudo yum install -y "${runtime_deps[@]}"
}

install_postgres() {
    set -x
    sudo yum install -y postgresql-server
    : ${PGDATA_PATH:=postgres.db}
    [[ -d "$PGDATA_PATH" ]] || initdb -D "$PGDATA_PATH"
    postgres -D "$PGDATA_PATH" -k /tmp &
}
