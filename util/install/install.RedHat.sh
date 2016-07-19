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
        bash
        coreutils
        git
        make
        rsync
        bzip2
        bzip2-devel
        xz
        flex
        sed
        mawk
        grep
        bc
        perl
        python
        java
        # bash
        bison
        # psycopg2
        readline-devel
        python-devel
        postgresql-devel
        # graphviz-devel
        autoconf pkgconfig libtool
        # mindbender
        ed
        # sampler
        #gcc-4.8
        cmake
        unzip
        numactl-devel
    )
    sudo yum install -y "${build_deps[@]}"
}

install__deepdive_runtime_deps() {
    set -x
    # install all runtime dependencies for DeepDive
    runtime_deps=(
        bash
        coreutils
        make
        rsync
        sed
        mawk
        grep
        bc
        perl
        python
        java
        gnuplot
        libtool-ltdl # for graphviz
        perl-Time-HiRes
    )
    sudo yum install -y "${runtime_deps[@]}"
    sudo localedef -f UTF-8 -i en_US en_US.UTF-8
}

install_postgres() {
    set -x
    sudo yum install -y postgresql-server
    : ${PGDATA_PATH:=postgres.db}
    [[ -d "$PGDATA_PATH" ]] || initdb -D "$PGDATA_PATH"
    postgres -D "$PGDATA_PATH" -k /tmp &
}
