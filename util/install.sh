#!/usr/bin/env bash
# Install DeepDive including its dependencies
set -eu
: ${BRANCH:=v0.6.x}
INSTALLER_HOME_URL=https://raw.github.com/HazyResearch/deepdive/$BRANCH/util/install
INSTALLER_HOME_DIR=$(dirname "$0")/install

running_from_git=true; [[ -e "$INSTALLER_HOME_DIR"/../../.git ]] || running_from_git=false
has() { type "$@"; } &>/dev/null
error() { echo "$@"; false; } >&2

# common installers ###########################################################
install_deepdive_build_deps() { false; }
install_deepdive_runtime_deps() { false; }
install_deepdive_git_repo() {
    if $running_from_git; then
        cd "$INSTALLER_HOME_DIR"/../..
    else
        git clone --recursive --branch $BRANCH https://github.com/HazyResearch/deepdive.git
        cd deepdive
    fi
    make
}
install_deepdive() {
    # TODO replace the following two lines with binary distribution
    run_installer_for deepdive_build_deps
    run_installer_for deepdive_git_repo || true
    run_installer_for deepdive_runtime_deps
}
################################################################################

# detect operating system
case $(uname) in
    Linux)
        if has apt-get && has debconf; then
            # Ubuntu/Debian
            os=Ubuntu
        # TODO support other Linux distros
        #elif [ -e /etc/redhat-release ]; then
        #    # CentOS/RedHat
        #    os=RedHat
        fi
        ;;

    Darwin)
        # Mac OS X
        os=Mac
        ;;

    *)
        error "$(uname): Unsupported Operating System"
esac
echo "### DeepDive installer for $os"
# load OS-specific install scripts located at install/install.*.sh
# each script defines bash functions whose names start with `install_`.
source_script() {
    local script=$1
    if [ -e "$INSTALLER_HOME_DIR/$script" ]; then
        source "$INSTALLER_HOME_DIR/$script"
    else
        # may be this script is run as a one-liner, get script from GitHub
        source <(set -x; curl -fsSL "$INSTALLER_HOME_URL/$script") "$@"
    fi
}
source_script install."$os".sh
source_os_script() { source_script install."$os"."$1".sh; }

# run selected installers, either interactively or via command-line arguments
list_installer_names() {
    # find installer names from all defined install_* functions
    declare -F | sed 's/^declare -f //; /^install_/!d; s/^install_//'
}
run_installer_for() {
    local name=$1
    for name; do
        local install_func="install_$name"
        if type "$install_func" &>/dev/null; then
            echo "## Starting installation for $name"
            if ( set +u; "$install_func" ); then
                echo "## Finished installation for $name"
            else
                error "## Failed installation for $name"
            fi
        else
            error "No such installer: $name"
        fi
    done
}
if [ $# -eq 0 ]; then
    if [ -t 0 ]; then
        # ask user what to install if input is a tty
        PS3="# Select what to install (enter a number or q to quit)? "
        select option in $(list_installer_names); do
            [ -n "$option" ] || break
            run_installer_for "$option" || continue
        done
    else
        # otherwise, show options
        echo "Specify what to install as command-line arguments:"
        list_installer_names
        false
    fi
else
    # what to install specified via command line arguments
    run_installer_for "$@"
fi
