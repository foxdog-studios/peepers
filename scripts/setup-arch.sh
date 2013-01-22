#!/bin/bash

set -o errexit
set -o nounset

install_aur=false
usage='
    Install dependencies and set up enviroment.

    Usage:

        # setup.sh [-a]

    -a  install AUR packages
'

while getopts :a opt; do
    case "${opt}" in
        a) install_aur=true ;;
        \?|*)
            echo "${usage}"
            exit 1
            ;;
    esac
done
unset opt usage

if $install_aur; then
    yaourt --needed --noconfirm -Sy \
            android-ndk \
            android-sdk \
            android-sdk-platform-tools \
            android-udev

    sudo gpasswd -a "$(whoami)" adbusers

fi
unset install_aur

