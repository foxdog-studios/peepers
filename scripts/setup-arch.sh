#!/bin/bash

# Copyright 2013 Foxdog Studios Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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
        chromium \
        gimp \
        git \
        jdk7-openjdk \
        jre7-openjdk \
        vlc

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

echo '
Manual steps:

    1) Reboot (for udev rules and new groups)
'
