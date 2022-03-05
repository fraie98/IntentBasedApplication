#!/usr/bin/sudo /usr/bin/python
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

from time import sleep
from spine_leaf import dcSpineLeafTopo

CONTROLLER_IP="127.0.0.1"
CONTROLLER_PORT="8080"
SPINES=2
LEAFS=3

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
#print net.pingAll()

print "adding intent connection"
r=requests.post("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/addNewIntent/json", 
	json={"host1_IP":"10.0.0.1", "host2_IP":"10.0.0.3", "timeout":1000000 })
r=requests.post("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/addNewIntent/json", 
	json={"host1_IP":"10.0.0.1", "host2_IP":"10.0.0.2", "timeout":1000000 })
print r.status_code
r=requests.get("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/getIntents/json")
print r.text
sleep(5)
# test ping functionality for all hosts again
print net.pingAll()
r=requests.post("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/delIntent/json", 
	json={"host1_IP":"10.0.0.1", "host2_IP":"10.0.0.3"})
sleep(5)
print net.pingAll()
net.configLinkStatus('s11','l21','down')
sleep(5)
print net.pingAll()
CLI( net )
net.stop()