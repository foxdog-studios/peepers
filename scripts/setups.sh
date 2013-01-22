#!/bin/bash

set -o errexit
set -o nounset

sudo yaourt --needed --noconfirm -Sy \
        android-sdk \
        android-sdk-platform-tools

