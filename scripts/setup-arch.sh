#!/bin/bash

set -o errexit
set -o nounset

yaourt --needed --noconfirm -Sy \
        android-ndk \
        android-sdk \
        android-sdk-platform-tools

