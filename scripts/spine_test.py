#!/usr/bin/python
import os
import sys

from mininet.clean import Cleanup
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.link import TCLink
from mininet.node import OVSSwitch,  RemoteController
import requests

from spine_leaf import dcSpineLeafTopo

CONTROLLER_IP="127.0.0.1"
CONTROLLER_PORT="8080"
SPINES=1
LEAFS=4

setLogLevel('info')
print "cleanup junk from old runs"
Cleanup.cleanup()
print "Create remote controller to which switches are attached"
rc = RemoteController( "c0", ip=CONTROLLER_IP)
topo = dcSpineLeafTopo(k=SPINES, l=LEAFS)
net = Mininet(  topo=topo, switch=OVSSwitch, build=False, link=TCLink,controller=rc )
print "building network"
net.build()
print "starting network"
net.start()

print "Dumping host connections"
dumpNodeConnections(net.hosts)

# test ping functionality for all hosts
print net.pingAll()

print "adding intent connection"
r=requests.post("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/addNewIntent/json", 
	json={"host1":"10.0.0.1", "host2":"10.0.0.2", "timeout":10,"isMac":"False" })
print r.status_code
r=requests.get("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/getIntents/json")
print r.text

# test ping functionality for all hosts again
print net.pingAll()

CLI( net )
net.stop()