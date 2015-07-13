#!/usr/bin/env bash
# DeepDive Installer
set -eu

: ${RELEASE:=v0.7.0}                        # the DeepDive release version to install
: ${PREFIX:=~/local/deepdive}               # the path to install deepdive

: ${INSTALLER_BRANCH:=${BRANCH:-v0.7.x}}    # the branch from which the installer scripts should be downloaded
INSTALLER_HOME_URL=https://github.com/HazyResearch/deepdive/raw/"${INSTALLER_BRANCH}"/util/install
INSTALLER_HOME_DIR=$(dirname "$0")/install

# run the correct installer directly from GitHub if BRANCH is specified
[[ -z "${BRANCH:-}" || -n "${INSTALLER_REMOTE_EXEC:-}" ]] ||
    INSTALLER_REMOTE_EXEC=true \
    exec bash <(set -x; curl -fsSL "${INSTALLER_HOME_URL}.sh") "$@"

running_from_git=true; [[ -e "$INSTALLER_HOME_DIR"/../../.git ]] || running_from_git=false
has() { type "$@"; } &>/dev/null
error() { echo "$@"; false; } >&2

# common installers ###########################################################
# installs DeepDive's build dependencies
install_deepdive_build_deps() { false; }
# installs DeepDive's runtime dependencies
install_deepdive_runtime_deps() { false; }
# fetches DeepDive source tree
install_deepdive_git_repo() {
    if $running_from_git; then
        cd "$INSTALLER_HOME_DIR"/../..
    else
        git clone --recursive --branch "$INSTALLER_BRANCH" https://github.com/HazyResearch/deepdive.git
        cd deepdive
    fi
}
# installs DeepDive from source by going through the full build
install_deepdive_from_source() {
    # prepare fetching and building source
    run_installer_for deepdive_build_deps
    run_installer_for deepdive_git_repo
    # install DeepDive from source
    make install PREFIX="$PREFIX"
    # install runtime dependencies
    run_installer_for deepdive_runtime_deps
}
# installs DeepDive with a release binary
install_deepdive_release() {
    local os=$(uname)
    local tarball="deepdive-${RELEASE}-${os}.tar.gz"
    local url="https://github.com/HazyResearch/deepdive/releases/download/${RELEASE}/$tarball"
    (
    # showing what is going on
    set -x
    rm -f "$tarball"
    # download tarball
    curl -fLRO "$url" || wget -N "$url"
    # unpack tarball
    mkdir -p "$PREFIX"
    tar xzvf "$tarball" -C "$PREFIX"
    ) || return $?
    echo
    echo "DeepDive release $RELEASE has been installed at $PREFIX"
    echo "Please add the following line to your ~/.bashrc:"
    echo "  export PATH=\"$PREFIX/bin:\$PATH\""
}
# installs DeepDive with a release binary and runtime dependencies
install_deepdive() {
    run_installer_for deepdive_release
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
