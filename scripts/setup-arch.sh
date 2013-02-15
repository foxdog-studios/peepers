#!/bin/bash

set -o errexit
set -o nounset

install_aur=false

usage='
    Install dependencies and set up environment

    Usage:

        # setup-arch.sh [-a]

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

sudo pacman --needed --noconfirm -Sy \
        apache-ant \
        git \

if $install_aur; then
    yaourt --needed --noconfirm -Sy \
            android-sdk \
            android-sdk-platform-tools \
            android-udev \

    sudo android update sdk --no-ui --filter android-10
    sudo systemctl enable adb.service
    sudo systemctl start adb.service

    sudo gpasswd -a "$(whoami)" adbusers
fi
unset install_aur

echo '
Manual steps:

    1) Reboot (for udev rules and new groups)
'
