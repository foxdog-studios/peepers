#!/usr/bin/bashir

cd ..
konsole -e vlc --fullscreen cfg/peepers.sdp --network-caching 0 &> /dev/null & disown

