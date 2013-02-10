#!/usr/bin/bashir

cd ..
konsole -e vlc cfg/peepers.sdp  --network-caching 0 &> /dev/null & disown

