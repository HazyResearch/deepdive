#!/usr/bin/env bash
# DeepDive Installer
set -euo pipefail

: ${RELEASE:=v0.8-STABLE}   # the DeepDive release version to install
: ${BRANCH:=v0.8.x}         # the DeepDive branch to clone, download, and build
: ${PREFIX:=~/local}        # the path to install deepdive
: ${GITCLONE:=deepdive}     # the path to clone deepdive's repo

: ${GITHUB_BASEURL:=https://github.com/HazyResearch/deepdive}
: ${INSTALLER_BRANCH:=master}   # the branch from which the installer scripts should be downloaded
INSTALLER_HOME_URL="$GITHUB_BASEURL"/raw/"$INSTALLER_BRANCH"/util/install
INSTALLER_HOME_DIR=$(dirname "$0")/install

# see if running from the git repo
: ${INSTALLER_LOCAL_FIRST:=false}
! [[ -e "$INSTALLER_HOME_DIR"/../../.git ]] || INSTALLER_LOCAL_FIRST=true
! $INSTALLER_LOCAL_FIRST ||
    # set GITCLONE to the containing git working copy when running from it
    case $(declare -p GITCLONE) in "declare --"*) false ;; *) true ;; esac ||
    GITCLONE="$INSTALLER_HOME_DIR"/../.. INSTALLER_BRANCH=HEAD BRANCH=HEAD

$INSTALLER_LOCAL_FIRST || # unless this is running directly from a git repo
# run the correct installer directly from GitHub unless already doing so
${INSTALLER_REMOTE_EXEC:-false} ||
    INSTALLER_REMOTE_EXEC=true \
    exec bash <(set -x; curl -fsSL "${INSTALLER_HOME_URL}.sh") "$@"

# use a fixed locale to ensure consistent behavior
export LC_ALL=en_US.UTF-8

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
download() {
    local url=$1
    local file=${2:-$(basename "$1")}
    file=$(python -c 'import urllib,sys;print urllib.unquote(sys.argv[1])' "$file")
    if [[ -e "$file" ]]; then
        echo "# File exists, skipping download from $url"
    else
        echo "# Downloading ${2:+$file from }$url"
        if type curl &>/dev/null; then
            curl -fLRo "$file" "$url"
        elif type wget &>/dev/null; then
            wget -N -O "$file" "$url"
        else
            error "No known method to download (curl or wget)"
        fi
    fi
}
timeout_or_do() {
    local timeout=${1:?Missing timeout in seconds}; shift
    local msg=${1:?Missing prompt message}; shift
    if [[ -t 0 ]]; then
        local key=
        if read -p "$msg" -t "$timeout" -n 1 -s key; echo; [[ -n "$key" ]]; then
            "$@"
        fi
    fi
}
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
    error "Please make sure the following packages are available on your system:$(
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
    # $GITCLONE already points to a git clone and the branch can be checked out
    { [[ -e "$GITCLONE"/.git ]] && (cd "$GITCLONE" && git checkout "$BRANCH"); } ||
    # or grab a clone with git
    git clone --recursive --branch "$BRANCH" "$GITHUB_BASEURL".git "$GITCLONE"
}
# installs DeepDive from source by going through the full build
install__deepdive_from_source_no_dependencies() {
    # clone git repo if necessary
    run_installer_for _deepdive_git_repo
    cd "$GITCLONE"
    echo "# Installing from DeepDive source at $PWD"
    # install DeepDive from source
    make install PREFIX="$PREFIX"
}
install__deepdive_from_source() {
    # prepare fetching and building source
    run_installer_for _deepdive_build_deps
    run_installer_for _deepdive_from_source_no_dependencies
    # install runtime dependencies
    run_installer_for _deepdive_runtime_deps
}
# utilities to allow selecting a different RELEASE interactively
timeout_or_select_release() {
    local forWhat=${1:?Missing description of release}; shift
    "$@"  # show what will happen with the current release
    timeout_or_do 2 "# Press any key to select a different release or Enter to proceed..." \
        select_release "$forWhat" "$@"
}
select_release() {
    local forWhat=${1:?Missing description of release}; shift
    PS3="# Select a release${forWhat:+ $forWhat}: "
    select release in $(list_deepdive_releases); do
        case $release in
            "") break ;;
            *)
                RELEASE=$release
                "$@"  # show what will happen with the selected release
                break
        esac
    done
}
list_deepdive_releases() {
    # find all links to a GitHub tree from the release page
    curl -fsSL "$GITHUB_BASEURL"/releases/ | sed '
        \:/.*/tree/:!d
        s/.*href="//; s/".*$//
        s:.*/tree/::
        s:/.*$::
    '
}
# installs DeepDive with a release binary
install_deepdive_from_release() {
    timeout_or_select_release "to install" \
        eval 'echo "# Installing DeepDive release $RELEASE..."'
    local os=$(uname)
    local arch=$(uname -m)
    case $os-$arch in
        Darwin-x86_64|\
        Linux-x86_64) ;; # we publish binaries for x86 (64bit) only
        *) error "$os-$arch: No binary release available for your OS" "DeepDive must be installed from source"
    esac
    local tarball="deepdive-${RELEASE}-${os}.tar.gz"
    local url="$GITHUB_BASEURL/releases/download/${RELEASE}/$tarball"
    (
    # showing what is going on
    set -x
    rm -f "$tarball"
    # download tarball
    download "$url"
    # unpack tarball
    mkdir -p "$PREFIX"
    tar xzvf "$tarball" -C "$PREFIX"
    ) || return $?
    echo
    echo "# DeepDive release $RELEASE has been installed at $PREFIX"
    echo "# Please add the following line to your ~/.bash_profile:"
    echo "export PATH=\"$PREFIX/bin:\$PATH\""
}
# installs DeepDive with a release binary and runtime dependencies
install_deepdive() {
    run_installer_for deepdive_from_release
    run_installer_for _deepdive_runtime_deps
}
# installs DeepDive examples and tests
install__deepdive_examples_tests() {
    timeout_or_select_release "to download examples and tests from" \
        eval 'echo "# Downloading examples and tests (from $RELEASE)..."'
    download_deepdive_github_tree "DeepDive examples and tests" \
        deepdive-${RELEASE#v} \
        examples test {compiler,runner,inference,shell,util{,/build}}/test
}
install_spouse_example() {
    timeout_or_select_release "to download spouse example from" \
        eval 'echo "# Downloading spouse example (from $RELEASE)..."'
    download_deepdive_github_tree "Spouse example DeepDive app" \
        spouse_example-${RELEASE#v} \
        examples/spouse
}
# launches tutorial notebook
install_deepdive_example_notebook() {
    : ${NOTEBOOK_BRANCH:=tutorial-in-a-notebook}
    : ${NOTEBOOK_PATH:=examples/spouse/DeepDive%20Tutorial%20-%20Extracting%20mentions%20of%20spouses%20from%20the%20news.ipynb}
    : ${NOTEBOOK_URL:=$GITHUB_BASEURL/raw/$NOTEBOOK_BRANCH/$NOTEBOOK_PATH}
    mkdir -p deepdive_notebooks
    cd deepdive_notebooks
    download "$NOTEBOOK_URL"
    PATH="$PREFIX/jupyter/bin:$PATH"
    type jupyter || run_installer_for _jupyter
    jupyter notebook *.ipynb
}
# launches a Jupyter notebook after installing it locally
install_jupyter_notebook() {
    PATH="$PREFIX/jupyter/bin:$PATH"
    type jupyter || run_installer_for _jupyter
    jupyter notebook
}
# installs a local Jupyter within a virtualenv with easy_install or pip from scratch
install__jupyter() {
    PATH="$PREFIX/jupyter/bin:$PATH"
    if ! [[ -x "$PREFIX"/jupyter/bin/jupyter ]]; then
        if ! [[ -x "$PREFIX"/jupyter/bin/pip ]]; then
            : ${PYTHONVERSION:=2.7}
            # easy_install should be available on Mac by default and many other Python installations
            if type easy_install-$PYTHONVERSION; then
                # use easy_install to bootstrap a virutalenv that has pip
                export PYTHONPATH="$PREFIX"/jupyter/lib/python$PYTHONVERSION/site-packages
                mkdir -p "$PYTHONPATH"
                easy_install-$PYTHONVERSION --prefix "$PREFIX"/jupyter virtualenv
            else # download get-pip.py to bootstrap a virtualenv
                mkdir -p "$PREFIX"/jupyter/bootstrap
                ( cd "$PREFIX"/jupyter/bootstrap
                    download https://bootstrap.pypa.io/get-pip.py
                    [[ -e virtualenv.py ]] ||
                    "$(type -p python)" get-pip.py virtualenv --ignore-installed --target .
                )
                virtualenv() { PYTHONPATH="$PREFIX"/jupyter/bootstrap \
                    "$(type -p python)" -c 'import virtualenv; virtualenv.main()' "$@"; }
            fi
            virtualenv "$PREFIX"/jupyter
        fi
        # use pip to install Jupyter
        pip install jupyter
    fi
    type jupyter
}
# how to download a subtree of DeepDive's GitHub repo
download_deepdive_github_tree() {
    local what=$1; shift
    local dest=$1; shift
    # the rest of the arguments are relative paths to download
    if [[ -s "$dest"/.downloaded ]]; then
        echo "# $what already downloaded at $(cd "$dest" && pwd)"
    else
        local tarballPrefix=deepdive-"${RELEASE#v}"
        local tmpdir="$dest".download
        mkdir -p "$tmpdir"
        set -x
        curl -fsSL "$GITHUB_BASEURL"/archive/"$RELEASE".tar.gz |
        tar xvzf - -C "$tmpdir" "${@/#/$tarballPrefix/}"
        ! [[ -e "$dest" ]] || mv -f "$dest" "$dest"~
        if [[ $# -eq 1 && -d "$tmpdir/$tarballPrefix/$1" ]]; then
            # don't nest things deep when downloading only one path
            mv -f "$tmpdir/$tarballPrefix/$1" "$dest"
        else
            mv -f "$tmpdir/$tarballPrefix" "$dest"
        fi
        date >"$dest"/.downloaded
        rm -rf "$tmpdir"
    fi
}
# runs tests against installed DeepDive
install_run_deepdive_tests() {
    run_installer_for _deepdive_examples_tests
    set -x
    PATH="$PREFIX/bin:$PATH" \
    deepdive env deepdive-${RELEASE#v}/test/test-installed.sh
}
################################################################################

# detect operating system
case $(uname) in
    Linux)
        if has apt-get && has debconf; then
            # Ubuntu/Debian
            os=Ubuntu
        # TODO support other Linux distros
        elif [[ -e /etc/redhat-release ]]; then
            # CentOS/RedHat
            os=RedHat
        else # unrecognized Linux distro
            os=
            # try to grab a Linux distro identifier
            {
                set -o pipefail
                hint=$(lsb_release -i | cut -f2) ||
                hint=$(. /etc/lsb-release && echo $DISTRIB_ID) ||
                hint=$(head -1 /etc/redhat-release) ||
                hint=  # give up finding hint
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
    if $INSTALLER_LOCAL_FIRST && [[ -e "$INSTALLER_HOME_DIR/$script" ]]; then
        source "$INSTALLER_HOME_DIR/$script"
    else
        # may be this script is run as a one-liner, get script from GitHub
        #source <(curl -fsSL "$INSTALLER_HOME_URL/$script") "$@"
        # XXX using a workaround since source with process substitution has problem in bash 3 (OS X default)
        # See: https://bugzilla.altlinux.org/show_bug.cgi?id=7475
        init_INSTALLER_TEMP_DIR
        local script_path="$INSTALLER_TEMP_DIR/$script"
        mkdir -p "$(dirname "$script_path")"
        curl -fsSL "$INSTALLER_HOME_URL/$script" >"$script_path"
        source "$script_path" "$@"
    fi
}
source_os_script() { source_script install."$os"."$1".sh; }
[[ -z "$os" ]] || source_script install."$os".sh

# run selected installers, either interactively or via command-line arguments
list_installer_names() {
    local show_only=${1:-visible}
    # find installer names from all defined install_* functions
    _list_installer_names() {
        declare -F | sed 's/^declare -f //; /^install_/!d; s/^install_//'
    }
    # unless the first argument is 'all', hide installers whose name begins with underscore
    case $show_only in
        all)
            list_installer_names visible
            list_installer_names hidden
            ;;
        hidden)
            _list_installer_names | sed '/^_/!d'
            ;;
        visible)
            _list_installer_names | sed '/^_/d'
            ;;
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
        set +e  # dont abort the select loop on installer error
        interactive_installer() {
            local show_only=${1:-visible}
            case $show_only in
                all)
                    PS3="# Install what (enter to repeat options, q to quit, or a number)? "
                    ;;
                *)
                    PS3="# Install what (enter to repeat options, a to see all, q to quit, or a number)? "
                    ;;
            esac
            select option in $(list_installer_names $show_only); do
                if [[ -n "$option" ]]; then
                    run_installer_for "$option"
                else
                    case $REPLY in
                        ""|q) # quit
                            break ;;
                        a) # show all options (including hidden ones)
                            case $show_only in
                                all) invalid_choice ;;
                                *) interactive_installer all; break ;;
                            esac ;;
                        *) invalid_choice
                    esac
                    continue
                fi
            done
        }
        invalid_choice() { error "$REPLY: Invalid option"; }
        interactive_installer
    else
        # otherwise, show options
        echo "# Specify what to install as command-line arguments:"
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
