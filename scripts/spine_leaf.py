from mininet.topo import Topo
from mininet.util import irange

######################################
######### Global Variables ###########
######################################
spineList = [ ]
leafList = [ ]
switchList = [ ]

######################################
###### Define topologies here ########
######################################

#Data center Spine Leaf Network Topology
class dcSpineLeafTopo(Topo):
    "Linear topology of k switches, with one host per switch."
    
    def __init__(self, k, l, **opts):
        """Init.
            k: number of switches (and hosts)
            hconf: host configuration options
            lconf: link configuration options"""

        super(dcSpineLeafTopo, self).__init__(**opts)

        self.k = k
        self.l = l
        for i in irange(0, k-1):
            spineSwitch = self.addSwitch('s%s%s' % (1,i+1))
            spineList.append(spineSwitch)

        for i in irange(0, l-1):
            leafSwitch = self.addSwitch('l%s%s' % (2, i+1))

            leafList.append(leafSwitch)
            host1 = self.addHost('h%s' % (i+1))
            #host12 = self.addHost('h%s' % (i+1))
            #hosts1 = [ net.addHost( 'h%d' % n ) for n in 3, 4 ]

            "connection of the hosts to the left tor switch "
            self.addLink(host1, leafSwitch)
            #self.addLink(host12, leafSwitch)

        for i in irange(0, k-1):
            for j in irange(0, l-1): #this is to go through the leaf switches
                self.addLink(spineList[i], leafList[j])
