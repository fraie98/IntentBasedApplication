#!/usr/bin/python
import os
import sys
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.link import TCLink
from mininet.node import OVSSwitch,  RemoteController
from spine_leaf import dcSpineLeafTopo

CONTROLLER_IP="127.0.0.1"
SPINES=1
LEAFS=10

"Create remote controller to which switches are attached"
setLogLevel('info')
rc = RemoteController( "c0", ip=CONTROLLER_IP)
topo = dcSpineLeafTopo(k=SPINES, l=LEAFS)
net = Mininet(  topo=topo, switch=OVSSwitch, build=False, link=TCLink,controller=rc )

print "building network"
net.build()
print "starting network"
net.start()

print "Dumping host connections"
dumpNodeConnections(net.hosts)

CLI( net )
net.stop()