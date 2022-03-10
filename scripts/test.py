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
from random import seed
from random import randint
#import matplotlib.pyplot as pl

def test(SPINES, LEAFS, N_HOSTS, N_HOSTS_TO_TEST, N, HOST_TESTED):
    CONTROLLER_IP="127.0.0.1"
    CONTROLLER_PORT="8080"
    # SPINES=2
    # LEAFS=3
    # N_HOSTS=4 # number of hosts per leaf
    O_RATIO=2 # oversubscription ratio
    setLogLevel('info')
    print "cleanup junk from old runs"
    Cleanup.cleanup()
    print "Create remote controller to which switches are attached"

    rc = RemoteController( "c0", ip=CONTROLLER_IP,  protocols='OpenFlow13')
    topo = dcSpineLeafTopo(k=SPINES, l=LEAFS, n=N_HOSTS, oRatio=O_RATIO)

    net = Mininet(  topo=topo, link=TCLink,build=False, controller=rc )
    print "building network"
    net.build()
    print "starting network"
    net.start()

    print "Dumping host connections"
    dumpNodeConnections(net.hosts)

    # test ping functionality for all hosts
    #print net.pingAll()

    dest=[]
    seed(10)
    counter=0
    for i in range(1,LEAFS+1):
            for j in range(1,N_HOSTS+1):
                counter+=1
                check=True
                while(check):
                    value = randint(1, N_HOSTS*LEAFS)
                    if(value!=counter):
                        check=False
                source="10.0.0."+str(counter)
                destination="10.0.0."+str(value)
                dest.append(destination)
                #print("h"+str(i)+str(j)+" "+source+" "+destination)
                print "adding intent connection for h"+str(i)+str(j)
                r=requests.post("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/addNewIntent/json",
                 json={"host1_IP":source, "host2_IP":destination, "timeout":1000000 })
                print r.status_code



    print "getting intents list"
    r=requests.get("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/getIntents/json")
    print r.text
    sleep(5)
    # test ping functionality for all hosts at the same time
    counter=0
    for i in range(1,LEAFS+1):
        exit=False
	for j in range(1,N_HOSTS+1):
            counter+=1
	    hostName='h%s%s' % (i,j)
            net.getNodeByName(hostName).sendCmd(  # non-blocking call
	        'ping',  "-c "+str(N),dest[counter-1], 
    	        '1> results/'+hostName+'.out 2>results/'+hostName+'.err &' ) # save results in temporary files
            if counter==N_HOSTS_TO_TEST:
                exit=True
                break
        if exit:
            break

    sleep(N+10)
    CLI( net )
    net.stop()

    somma=0
    hostName=HOST_TESTED
    f = open("results/"+hostName+".out", "r")
    for x in f:
        if counter==1:
            continue
        if "time" in x and "ttl" in x:
            a=x.split(" ")
            b=a[6].split("=")
            somma+=float(b[1])
    avg=somma/N

    return avg

if __name__ == "__main__":
    host="h11"
    avg=[]
    avg.append(test(2,3,4,1,10,host))
    avg.append(test(2,3,4,4,10,host))
    avg.append(test(2,3,4,8,10,host))
    avg.append(test(2,3,4,12,10,host))
    print avg
