#!/bin/bash

set -o errexit
set -o nounset

function usage
{
    echo '
    Uninstall, clean, build, and/or install Peepers

    Usage:

        # build.sh [-bciu]

    -b  build
    -c  clean
    -i  install
    -u  uninstall
'
    exit 1
}

if [[ $# -eq 0 ]]; then
    usage
fi

build=false
clean=false
install=false
uninstall=false

while getopts :bciu opt; do
    case "${opt}" in
        b) build=true ;;
        c) clean=true ;;
        i) install=true ;;
        u) uninstall=true ;;
        \?|*) usage ;;
    esac
done
unset opt usage

readonly REPO="$(
    realpath -- "$(
        dirname -- "$(
            realpath -- "${BASH_SOURCE[0]}"
        )"
    )/.."
)"

cd -- "${REPO}"

if $uninstall; then
    adb uninstall com.foxdogstudios.peepers
fi
unset uninstall

if $clean; then
    ant clean
fi
unset clean

if $build; then
    ant debug
fi
unset build

if $install; then
    ant installd
fi
unset install

