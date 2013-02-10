#!/usr/bin/env python2

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from argparse import ArgumentParser
from struct import *
import datetime
import os
import socket
import sys

import pcapy


DESCRIPTION = """
Listen for MJPEG packets and save the jpegs.
"""[1:]


class MJpegCaptor(object):
    def __init__(self, device, port, output_dir):
        self.device = device
        self.port = port
        self.output_dir = output_dir
        self.jpegs_written = 0
        self.write_jpeg = False
        self.jpeg = []

    def capture(self):
        cap = pcapy.open_live(self.device, 65536 , 1 , 0)

        #start sniffing packets
        while(1) :
            (header, packet) = cap.next()
            self.parse_packet(packet)

    #Convert a string of 6 characters of ethernet address into a dash separated hex string
    def eth_addr(self, a):
        b = "%.2x:%.2x:%.2x:%.2x:%.2x:%.2x" % (ord(a[0]), ord(a[1]), ord(a[2]), ord(a[3]),
                                               ord(a[4]), ord(a[5]))
        return b

    #function to parse a packet
    def parse_packet(self, packet) :

        #parse ethernet header
        eth_length = 14

        eth_header = packet[:eth_length]
        eth = unpack('!6s6sH' , eth_header)
        eth_protocol = socket.ntohs(eth[2])
        #Parse IP packets, IP Protocol number = 8
        if eth_protocol == 8 :
            #Parse IP header
            #take first 20 characters for the ip header
            ip_header = packet[eth_length:20+eth_length]

            #now unpack them :)
            iph = unpack('!BBHHHBBH4s4s' , ip_header)

            version_ihl = iph[0]
            version = version_ihl >> 4
            ihl = version_ihl & 0xF

            iph_length = ihl * 4

            ttl = iph[5]
            protocol = iph[6]
            s_addr = socket.inet_ntoa(iph[8]);
            d_addr = socket.inet_ntoa(iph[9]);

            if protocol == 17 :
                u = iph_length + eth_length
                udph_length = 8
                udp_header = packet[u:u+8]

                #now unpack them :)
                udph = unpack('!HHHH' , udp_header)

                source_port = udph[0]
                dest_port = udph[1]

                if dest_port != self.port:
                    return
                length = udph[2]
                checksum = udph[3]

                h_size = eth_length + iph_length + udph_length
                data_size = len(packet) - h_size

                #get data from the packet
                data = packet[h_size:]

                self.parse_rtp_data(data)

    def parse_rtp_data(self, data):
        rtp_header = data[:12]
        jpeg_header = data[12:20]
        jpeg = data[20:]
        if self.write_jpeg:
            self.jpeg.extend(jpeg)
        if self.check_jpeg_frame_end(rtp_header):
            if self.write_jpeg:
                self.save_jpeg()
            else:
                self.write_jpeg = True

    def save_jpeg(self):
        jpeg = bytearray(self.jpeg)
        with open(os.path.join(self.output_dir, '%d.jpg' % (self.jpegs_written,)), 'wb') \
                as jpg_file:
            jpg_file.write(jpeg)
        self.jpegs_written += 1

    def check_jpeg_frame_end(self, rtp_header):
        return ord(rtp_header[1]) > 26

    def print_header(self, header):
        print([ord(b) for b in header])

    def find_start_of_image(self, jpeg):
        for i, b in enumerate(jpeg):
            if type(b) != int:
                b_ord = ord(b)
            else:
                b_ord = b
            if b_ord == 255 and i < len(jpeg):
                if ord(jpeg[i + 1])== 216:
                    print('Found SOI at index %s' % i)
                    return
        print('No SOI found')

def create_argument_parser():
    argument_parser = ArgumentParser(description=DESCRIPTION)
    add_argument = argument_parser.add_argument
    add_argument('-p', '--port', help='The capture port', type=int, default=1024)
    add_argument('device', help='The name of the capture device to use')
    add_argument('output_dir', help='The directory to output the JPEGs to')
    return argument_parser


def main(argv=None):
    if argv is None:
        argv = sys.argv
    args = create_argument_parser().parse_args(args=argv[1:])
    mjpeg_captor = MJpegCaptor(args.device, args.port, args.output_dir)
    mjpeg_captor.capture()


if __name__ == '__main__':
    exit(main())

