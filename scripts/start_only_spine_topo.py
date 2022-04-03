#!/usr/bin/sudo /usr/bin/python
import os
import sys

from mininet.clean import Cleanup
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.node import OVSSwitch,  RemoteController
from mininet.link import TCLink
from mininet.util import irange
import requests

from time import sleep
from spine_leaf import dcSpineLeafTopo

CONTROLLER_IP="127.0.0.1"
CONTROLLER_PORT="8080"
SPINES=2
LEAFS=3
N_HOSTS=2
setLogLevel('info')
print "cleanup junk from old runs"
Cleanup.cleanup()
print "Create remote controller to which switches are attached"
rc = RemoteController( "c0", ip=CONTROLLER_IP,  protocols='OpenFlow13')
topo = dcSpineLeafTopo(k=SPINES, l=LEAFS, n=N_HOSTS)
net = Mininet(  topo=topo, link=TCLink,build=False, controller=rc )
print "building network"
net.build()
print "starting network"
net.start()

print "Dumping host connections"
dumpNodeConnections(net.hosts)
r=requests.post("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/addNewIntent/json", 
	json={"host1_IP":"10.0.0.1", "host2_IP":"10.0.0.3", "timeout":1000000 })
r=requests.post("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/addNewIntent/json", 
	json={"host1_IP":"10.0.0.1", "host2_IP":"10.0.0.2", "timeout":1000000 })
CLI(net)