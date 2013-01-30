#!/usr/bin/bashir

cd ..
konsole -e vlc peepers.sdp --network-caching=100 &> /dev/null & disown

