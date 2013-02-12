#!/usr/bin/bashir

cd ..
konsole -e ant -buildfile tools/localstreamer/build.xml run &> /dev/null & disown

