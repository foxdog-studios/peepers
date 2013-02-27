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

if [[ "${#}" == 0 ]]; then
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
    stdout="$(mktemp)"
    ant installd | tee "${stdout}"
    grep -q Success "${stdout}"
    rm -f $stdout
    unset stdout
fi
unset install

