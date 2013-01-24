#!/bin/bash

konsole -e vlc udp://@:9000 --network-caching=100 &> /dev/null & disown

