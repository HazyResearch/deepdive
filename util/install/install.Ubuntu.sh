#!/usr/bin/env bash
# DeepDive installers for Debian/Ubuntu Linux

LSB=$(lsb_release -ir 2>/dev/null | cut -f2) ||
LSB=$(. /etc/lsb-release 2>/dev/null  && echo $DISTRIB_ID $DISTRIB_RELEASE) ||
LSB=$(. /etc/os-release  2>/dev/null  && echo $PRETTY_NAME) ||
true
case ${LSB} in
    Debian*8*|Debian*7*) true ;;
    Ubuntu*12.04*|Ubuntu*14.04*|Ubuntu*15.04*|Ubuntu*16.04*) true ;;
    *) error "$LSB found: This installer may not work on your OS." ||
        error "It has been tested only on Debian 7 and 8, Ubuntu 12.04, 14.04, 15.04, and 16.04." ||
        true  # don't fail here as it might work for other versions
esac
is_debian=false; case ${LSB} in Debian*) is_debian=true; esac

install__deepdive_build_deps() {
    set -x
    build_deps=()
    sudo apt-get update
    sudo apt-get install -qy software-properties-common python-software-properties
    if $is_debian; then
        case $(cat /etc/debian_version) in
            8.*) # jessie-backports is needed
                sudo add-apt-repository -y "deb http://cdn-fastly.deb.debian.org/debian jessie-backports main"
                sudo apt-get update
                ;;
        esac
        build_deps+=(
        )
    else
        sudo add-apt-repository -y ppa:ubuntu-toolchain-r/test  # for gcc >= 4.8 on Precise (12.04)
        sudo add-apt-repository -y ppa:openjdk-r/ppa  # for openjdk 8
        sudo apt-get update
        build_deps+=(
            # sampler
            gcc-4.8
            g++-4.8
        )
    fi
    build_deps+=(
        build-essential
        bash
        coreutils
        git
        make
        rsync
        bzip2
        libbz2-dev
        xz-utils
        flex
        openjdk-8-jdk
        sed
        mawk
        grep
        bc
        perl
        python-software-properties
        # bash
        bison
        # psycopg2
        libreadline-dev
        python-dev
        libpq-dev
        # graphviz-devel
        autoconf pkg-config libtool
        # mindbender
        ed
        # sampler
        cmake
        unzip
        libnuma-dev
    )
    sudo apt-get install -qy "${build_deps[@]}"
}

install__deepdive_runtime_deps() {
    set -x
    runtime_deps=()
    # install all runtime dependencies for DeepDive
    sudo apt-get update
    sudo apt-get install -qy software-properties-common python-software-properties
    if $is_debian; then
        case $(cat /etc/debian_version) in
            8.*) # jessie-backports is needed for openjdk-8
                sudo add-apt-repository -y "deb http://cdn-fastly.deb.debian.org/debian jessie-backports main"
                sudo apt-get update
                ;;
        esac
    else
        sudo add-apt-repository -y ppa:openjdk-r/ppa  # for openjdk-8
        sudo apt-get update
    fi
    runtime_deps+=(
        bash
        coreutils
        bsdmainutils    # for column
        make
        rsync
        bc
        sed
        grep
        mawk
        perl
        python-software-properties
        openjdk-8-jre-headless
        gnuplot
        libltdl7  # for graphviz
        libnuma1
    )
    sudo apt-get install -qy "${runtime_deps[@]}"
    sudo locale-gen en_US.UTF-8
}

install_postgres_xl() {
    source_os_script pgxl
}

install_postgres() {
    set -x
    sudo apt-get update
    sudo apt-get install -qy postgresql
    local pgversion=$(ls -1 /var/lib/postgresql/ | head -n 1)
    if [ -z "${TRAVIS:-}" ]; then
        # add user to postgresql and trust all connections to localhost
        sudo -u postgres dropuser --if-exists $USER || sudo -u postgres dropuser $USER || true
        sudo -u postgres createuser --superuser $USER || true
        tmp=$(mktemp /tmp/pg_hba.conf.XXXXXXX)
        trap "rm -f $tmp" EXIT
        {
            echo 'host	all	all	127.0.0.1/32	trust'
            echo 'host	all	all	::1/128	trust'
            sudo cat /etc/postgresql/$pgversion/main/pg_hba.conf
        } >$tmp
        sudo tee /etc/postgresql/$pgversion/main/pg_hba.conf <$tmp >/dev/null
        sudo service postgresql restart
    fi
}
