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
import matplotlib.pyplot as plt
import numpy as np

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
    sleep(10)
    # test ping functionality for all hosts at the same time
    counter=0
    for i in range(1,LEAFS+1):
        exit=False
	for j in range(1,N_HOSTS+1):
            counter+=1
	    hostName='h%s%s' % (i,j)
            net.getNodeByName(hostName).sendCmd(  # non-blocking call
	        'ping',  "-c "+str(N),dest[counter-1], 
    	        '1> results/'+hostName+'_ping_'+str(N_HOSTS_TO_TEST)+'.out 2>results/'+hostName+'_ping_'+str(N_HOSTS_TO_TEST)+'.err &' ) # save results in temporary files
            if counter==N_HOSTS_TO_TEST:
                exit=True
                break
        if exit:
            break

    sleep(N+10)
    CLI( net )
    net.stop()

    hostName=HOST_TESTED
    avg=[]

    # compiute avg without first ping
    for h in range(N_HOSTS_TO_TEST):
        somma=0
        n_ping_contati=0
        f = open("results/"+hostName[h]+'_ping_'+str(N_HOSTS_TO_TEST)+".out", "r")
        for x in f:
            #if counter==1:
                #continue
            if "time" in x and "ttl" in x:
                if "seq=1 " in x:
                    continue
                if "Destination Host Unreachable" in x:
                    continue
                a=x.split(" ")
                b=a[6].split("=")
	        somma+=float(b[1])
                n_ping_contati=n_ping_contati+1
        if n_ping_contati==0:
            avg.append(0)
        else:
            avg.append(somma/(n_ping_contati))

    # compiute avg with first ping
    for h in range(N_HOSTS_TO_TEST):
        somma=0
        n_ping_contati=0
        f = open("results/"+hostName[h]+'_ping_'+str(N_HOSTS_TO_TEST)+".out", "r")
        for x in f:
            #if counter==1:
               #continue
            if "time" in x and "ttl" in x:
                if "Destination Host Unreachable" in x:
                    continue
                a=x.split(" ")
                b=a[6].split("=")
                somma+=float(b[1])
                n_ping_contati=n_ping_contati+1
        if n_ping_contati==0:
            avg.append(0)
        else:
            avg.append(somma/(n_ping_contati))

    res = requests.post("http://"+CONTROLLER_IP+":"+CONTROLLER_PORT+"/lb/deleteAllIntents/json")
    print "Test ended: delete all intents - " + str(res)

    return avg

if __name__ == "__main__":

    hosts=["h11","h12","h13","h14","h21","h22","h23","h24","h31","h32","h33","h34"]
    h11_values_no_first=[]
    h11_values_complete=[]

    avg=test(2,3,4,1,10,hosts)
    avg_1_complete=avg[1]
    avg_1_no_first=avg[0]
    h11_values_no_first.append(avg_1_no_first)
    h11_values_complete.append(avg_1_complete)

    avg=test(2,3,4,4,10,hosts)
    avg_4_complete=avg[4:]
    avg_4_no_first=avg[:4]
    h11_values_no_first.append(avg_4_no_first[0])
    h11_values_complete.append(avg_4_complete[0])

    avg=test(2,3,4,8,10,hosts)
    avg_8_complete=avg[8:]
    avg_8_no_first=avg[:8]
    h11_values_no_first.append(avg_8_no_first[0])
    h11_values_complete.append(avg_8_complete[0])

    avg=test(2,3,4,12,10,hosts)
    avg_12_complete=avg[12:]
    avg_12_no_first=avg[:12]
    h11_values_no_first.append(avg_12_no_first[0])
    h11_values_complete.append(avg_12_complete[0])

    print "-- 1 ping --"
    print avg_1_complete
    print avg_1_no_first

    print "-- 4 ping --"
    print avg_4_complete
    print avg_4_no_first

    print "-- 8 ping --"
    print avg_8_complete
    print avg_8_no_first

    print "-- 12 ping --"
    print avg_12_complete
    print avg_12_no_first

    print "-- plots host mean time --"
    name_new=[]
    for i in range(1,13,1):
        name_new.append("host"+str(i))

    x = np.arange(len(name_new))
    width = 0.35
    fig, ax = plt.subplots(figsize=(15,10))
    ax.bar(x - width/2, avg_12_no_first, width, label='No first',data=name_new)
    ax.bar(x + width/2, avg_12_complete, width, label='Complete',data=name_new)
    ax.set_ylabel('Time')
    ax.set_title('Mean time with and without first ping')
    ax.set_xticks(x)
    ax.set_xticklabels(name_new)
    ax.legend()
    fig.tight_layout()
    plt.show()

    print "-- plots h11 mean time --"
    colors1=['royalblue','orange','tomato','limegreen']
    colors2=['darkblue','darkorange','firebrick','darkgreen']
    name_new2=[]
    name_new2=["h11-1","h11-4","h11-8","h11-12"]
    x = np.arange(len(name_new2))
    width = 0.20
    fig, ax = plt.subplots(figsize=(10,10))
    ax.bar(x-width/2, h11_values_no_first,width,color=colors1)
    ax.bar(x+width/2, h11_values_complete,width,color=colors2)
    ax.set_ylabel('Time')
    ax.set_title('h11 with differente numbers of hosts pinging')
    ax.set_xticks(x, name_new2)
    fig.tight_layout()
    plt.show()
