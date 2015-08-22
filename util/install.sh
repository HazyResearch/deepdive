#!/usr/bin/env bash
# DeepDive Installer
set -eu

: ${RELEASE:=v0.7.0}    # the DeepDive release version to install
: ${PREFIX:=~/local}    # the path to install deepdive

: ${INSTALLER_BRANCH:=${BRANCH:-v0.7.x}}    # the branch from which the installer scripts should be downloaded
INSTALLER_HOME_URL=https://github.com/HazyResearch/deepdive/raw/"${INSTALLER_BRANCH}"/util/install
INSTALLER_HOME_DIR=$(dirname "$0")/install

# run the correct installer directly from GitHub if BRANCH is specified
[[ -z "${BRANCH:-}" || -n "${INSTALLER_REMOTE_EXEC:-}" ]] ||
    INSTALLER_REMOTE_EXEC=true \
    exec bash <(set -x; curl -fsSL "${INSTALLER_HOME_URL}.sh") "$@"

running_from_git=true; [[ -e "$INSTALLER_HOME_DIR"/../../.git ]] || running_from_git=false
has() { type "$@"; } &>/dev/null
error() {
    if [ -t 1 ]; then
        # on tty output, color errors in red
        echo -ne '\033[31m'
        echo "$@"
        echo -ne '\033[0m'
    else
        echo "$@"
    fi
    false
} >&2
INSTALLER_TEMP_DIR=
init_INSTALLER_TEMP_DIR() {
    # makes sure INSTALLER_TEMP_DIR points to a temporary directory
    if ! [[ -d "$INSTALLER_TEMP_DIR" ]]; then
        INSTALLER_TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}"/deepdive-installer.XXXXXXX)
        trap "rm -rf $INSTALLER_TEMP_DIR" EXIT
    fi
}

# common installers ###########################################################
# installs DeepDive's build dependencies
install__deepdive_build_deps() {
    local deps=
    deps=(
        git
        make
        bzip2
        unzip
        jdk
    )
    error "Please make sure the following packages are available on your system: $(
        printf '\n  %s' "${deps[@]}")" ||
            [[ ! -t 1 ]] || read -p "# Continue? " || true
}
# installs DeepDive's runtime dependencies
install__deepdive_runtime_deps() {
    local deps=
    deps=(
        bash
        coreutils
        xargs
        bc
        gnuplot
        jre
        python
    )
    error "Please make sure the following packages are available on your system:$(
        printf '\n  %s' "${deps[@]}")" ||
            [[ ! -t 1 ]] || read -p "# Continue? " || true
}
# fetches DeepDive source tree
install__deepdive_git_repo() {
    $running_from_git ||
        git clone --recursive --branch "$INSTALLER_BRANCH" https://github.com/HazyResearch/deepdive.git
}
# installs DeepDive from source by going through the full build
install_deepdive_from_source() {
    # prepare fetching and building source
    run_installer_for _deepdive_build_deps
    # install DeepDive from source
    if $running_from_git; then
        cd "$INSTALLER_HOME_DIR"/../..
        echo "# DeepDive source already at $PWD"
    else
        run_installer_for _deepdive_git_repo
        cd deepdive
        echo "# DeepDive source cloned at $PWD"
    fi
    make install PREFIX="$PREFIX"
    # install runtime dependencies
    run_installer_for _deepdive_runtime_deps
}
# installs DeepDive with a release binary
install_deepdive_from_release() {
    # TODO allow overriding RELEASE interactively
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
    echo "Please add the following line to your ~/.bash_profile:"
    echo "  export PATH=\"$PREFIX/bin:\$PATH\""
}
# installs DeepDive with a release binary and runtime dependencies
install_deepdive() {
    run_installer_for deepdive_from_release
    run_installer_for _deepdive_runtime_deps
}
################################################################################

# detect operating system
case $(uname) in
    Linux)
        if has apt-get && has debconf; then
            # Ubuntu/Debian
            os=Ubuntu
        # TODO support other Linux distros
        #elif [[ -e /etc/redhat-release ]]; then
        #    # CentOS/RedHat
        #    os=RedHat
        else # unrecognized Linux distro
            os=
            # try to grab a Linux distro identifier
            {
                set -o pipefail
                hint=$(lsb_release -i | cut -f2) ||
                hint=$(head -1 /etc/redhat-release)
            } 2>/dev/null
            error "WARNING: Unsupported GNU/Linux distribution${hint:+: $hint}"
        fi
        ;;

    Darwin)
        # Mac OS X
        os=Mac
        ;;

    *)
        # unsupported OS
        os=
        error "WARNING: Unsupported Operating System: $(uname)"
esac ||
    error "To build and install DeepDive correctly on your system, see:" ||
    error "  http://deepdive.stanford.edu/doc/advanced/developer.html#build-test" ||
    error "Beware that the following installer options may not work." ||
    true

echo "### DeepDive installer${os:+ for $os}"
# load OS-specific install scripts located at install/install.*.sh
# each script defines bash functions whose names start with `install_`.
source_script() {
    local script=$1
    if $running_from_git && [[ -e "$INSTALLER_HOME_DIR/$script" ]]; then
        source "$INSTALLER_HOME_DIR/$script"
    else
        # may be this script is run as a one-liner, get script from GitHub
        #source <(set -x; curl -fsSL "$INSTALLER_HOME_URL/$script") "$@"
        # XXX using a workaround since source with process substitution has problem in bash 3 (OS X default)
        # See: https://bugzilla.altlinux.org/show_bug.cgi?id=7475
        init_INSTALLER_TEMP_DIR
        local script_path="$INSTALLER_TEMP_DIR/$script"
        mkdir -p "$(dirname "$script_path")"
        (set -x; curl -fsSL "$INSTALLER_HOME_URL/$script" >"$script_path")
        source "$script_path" "$@"
    fi
}
source_os_script() { source_script install."$os"."$1".sh; }
[[ -z "$os" ]] || source_script install."$os".sh

# run selected installers, either interactively or via command-line arguments
list_installer_names() {
    local show_only=${1:-visible}
    # find installer names from all defined install_* functions
    declare -F | sed 's/^declare -f //; /^install_/!d; s/^install_//' |
    # unless the first argument is 'all', hide installers whose name begins with underscore
    case ${show_only:-visible} in
        all) cat ;;
        *) sed '/^_/d'
    esac
}
run_installer_for() {
    (
    # XXX running this in a context where set -e (errexit) is ignored, e.g., in
    # if, while, ||, or && command lists (except the last one) is problematic
    # since the install_* functions will continue running even upon errors.
    set +e; (set -e; false; true); local errexit_effective=$?; set -e
    if [[ $errexit_effective -eq 0 ]]; then
        read line file <<<$(caller)
        error "INSTALLER INTERNAL ERROR: $file:$line: "'run_installer_for cannot be run in a context where `set -e` is ignored.'
        return 1
    fi
    # run installer by name
    local name=$1
    local install_func="install_$name"
    if type "$install_func" &>/dev/null; then
        echo "## Starting installation for $name"
        # isolate install function in a subshell
        set +e; (set -e +u; "$install_func"); local c=$?; set -e
        if [[ $c -eq 0 ]]; then
            echo "## Finished installation for $name"
        else
            error "## Failed installation for $name" || true
            return $c
        fi
    else
        error "No such installer: $name"
    fi
    )
}
if [[ $# -eq 0 ]]; then
    if [[ -t 0 ]]; then
        # ask user what to install if input is a tty
        PS3="# Select what to install (enter for all options, q to quit, or a number)? "
        set +e  # dont abort the select loop on installer error
        select option in $(list_installer_names); do
            [[ -n "$option" ]] || break
            run_installer_for "$option"
        done
    else
        # otherwise, show options
        echo "Specify what to install as command-line arguments:"
        list_installer_names all
        # TODO show what each installer does
        false
    fi
else
    # what to install specified via command line arguments
    for name; do
        run_installer_for "$name"
    done
fi
