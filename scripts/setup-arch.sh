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

sudo pacman --needed --noconfirm -Sy \
        git \
        kdebase-konsole \
        python \
        python2 \
        python2-distribute \
        vlc

sudo easy_install-2.7 "http://corelabs.coresecurity.com/index.php?module=Wiki&action=attachment&type=tool&page=Pcapy&file=pcapy-0.10.8.tar.gz"

if $install_aur; then
    yaourt --needed --noconfirm -Sy \
            android-ndk \
            android-sdk \
            android-sdk-platform-tools \
            android-udev \
            javacv

    sudo gpasswd -a "$(whoami)" adbusers

fi
unset install_aur

# Install bashir
if ! which bashir &> /dev/null ; then
    cd /tmp
    git clone gitolite@foxdogstudios.com:bashir
    bashir/scripts/install.sh
    rm -fr bashir
fi

