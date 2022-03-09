from mininet.topo import Topo
from mininet.util import irange
######################################
######### Global Variables ###########
######################################
spineList = [ ]
leafList = [ ]
switchList = [ ]

hostBw = 100
######################################
###### Define topologies here ########
######################################

#Data center Spine Leaf Network Topology
class dcSpineLeafTopo(Topo):
    "Linear topology of k switches, with one host per switch."
    
    def __init__(self, k, l,n,oRatio=2, **opts):
        """Init.
            k: number of switches (and hosts)
            hconf: host configuration options
            lconf: link configuration options"""

        super(dcSpineLeafTopo, self).__init__(**opts)
        switchBw=(hostBw*n)//oRatio
        self.k = k
        self.l = l
        hierarchy=1 # NECESSARY! 1st digit of switch name is used by floodlight  
        for i in irange(1, k):
            spineSwitch = self.addSwitch('spine%s%s' % (hierarchy,i))
            spineList.append(spineSwitch)

        hierarchy+=1    
        for i in irange(1, l):
            leafSwitch = self.addSwitch('leaf%s%s' % (hierarchy,i))
            leafList.append(leafSwitch)
            for u in irange(1, n):
                host1 = self.addHost('h%s%s' % (i,u))
                "connection of the hosts to the left tor switch "
                self.addLink(host1, leafSwitch, bw=hostBw)

        for i in irange(0, k-1):
            for j in irange(0, l-1): #this is to go through the leaf switches
                self.addLink(spineList[i], leafList[j], bw=switchBw)
