#!/bin/bash

set -o errexit
set -o nounset

install_aur=false

usage='
    Install dependencies and set up enviroment

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
        kdebase-konsole \
        python2 \
        python2-distribute \
        vlc

# XXX: VLC hard crashes if this isn't done. This can probably be
# removed in the future when the vlc package is fixed.
sudo /usr/lib/vlc/vlc-cache-gen -f usr/lib/vlc/plugins

sudo easy_install-2.7 "http://corelabs.coresecurity.com/index.php?module=Wiki&action=attachment&type=tool&page=Pcapy&file=pcapy-0.10.8.tar.gz"

if $install_aur; then
    yaourt --needed --noconfirm -Sy \
            android-sdk \
            android-sdk-platform-tools \
            android-udev \
            javacv

    sudo android update sdk --no-ui --filter android-10
    sudo systemctl enable adb.service
    sudo systemctl start adb.service

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

echo '
Manual steps:

    1) Reboot (for udev rules and new groups)
'
