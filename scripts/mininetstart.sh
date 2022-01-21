#!/bin/sh
sudo mn --topo tree,3 --mac \
--switch ovsk --controller remote,ip=127.0.0.1,port=6653,protocols=OpenFlow13\
--ipbase=10.0.0.0 -v info
