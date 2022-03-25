#!/usr/bin/python2
import matplotlib.pyplot as plt
import numpy as np
import scipy.stats as st

DHU = 0
dups = 0
confidence = 0.8
FIRST_PING_ONLY_H11 = False

# compute the sample mean and the mean error (simmetric) for a given confidence
def mean_confidence_interval(data, confidence=0.95):
    a = 1.0 * np.array(data)
    n = len(a)
    m, se = np.mean(a), st.sem(a)
    h = se * st.t.ppf((1 + confidence) / 2., n-1)
    return m, h

def calc(N_HOSTS_TO_TEST, HOST_TESTED):
    hostName=HOST_TESTED
    avgWo1=[]
    first=[]
    conf=[]
    global DHU
    global dups
    # compiute avg without first ping
    for h in range(N_HOSTS_TO_TEST):
        times=[]
        f = open("results/"+hostName[h]+'_ping_'+str(N_HOSTS_TO_TEST)+".out", "r")
        for x in f:
            #if counter==1:
                #continue
            if "seq" in x :
                if "Destination Host Unreachable" in x:
                    DHU+=1
                    continue
                if "DUP!" in x:
                    dups+=1
                    continue
                a=x.split(" ")
                b=a[6].split("=")
                times.append(float(b[1]))
        if len(times)>1:
            avg, conf_int=mean_confidence_interval(times[1:],confidence )
            avgWo1.append(avg)
            conf.append(conf_int)
            first.append(times[0])  

    return avgWo1, first, conf

def compute_avg_first_ping(avg_x_complete):
    counter = 0
    sum_first_ping = 0
    for i in avg_x_complete:
    	sum_first_ping += i
    	counter += 1
    return sum_first_ping/counter 


if __name__ == "__main__":

    global DHU
    global dups
    global FIRST_PING_ONLY_H11
    hosts = ["h11","h12","h13","h14","h21","h22","h23","h24","h31","h32","h33","h34"]
    h11_values_no_first = []
    values_complete = []
    h11_values_conf = []
    conf_complete=[0]
    avg_1_no_first, avg_1_complete, conf1=calc(1,hosts)

    h11_values_no_first.append(avg_1_no_first)
    values_complete.append(avg_1_complete)
    h11_values_conf.append(conf1)

    avg_4_no_first, avg_4_complete,conf4=calc(4,hosts)
    h11_values_no_first.append(avg_4_no_first[0])
    if FIRST_PING_ONLY_H11:
        values_complete.append(avg_4_complete[0])
    else:
        avg_4_first_ping, conf = mean_confidence_interval(avg_4_complete)   
        values_complete.append(avg_4_first_ping)
        conf_complete.append(conf)
    h11_values_conf.append(conf4[0])

    avg_8_no_first, avg_8_complete,conf8=calc(8,hosts)
    h11_values_no_first.append(avg_8_no_first[0])
    if FIRST_PING_ONLY_H11:
        values_complete.append(avg_8_complete[0])
    else:
        avg_8_first_ping, conf = mean_confidence_interval(avg_8_complete)   
        values_complete.append(avg_8_first_ping)
        conf_complete.append(conf)
    h11_values_conf.append(conf8[0])


    avg_12_no_first, avg_12_complete, conf12=calc(12,hosts)
    h11_values_no_first.append(avg_12_no_first[0])
    if FIRST_PING_ONLY_H11:
        values_complete.append(avg_12_complete[0])
    else:
        avg_12_first_ping, conf = mean_confidence_interval(avg_12_complete)   
        values_complete.append(avg_12_first_ping)
        conf_complete.append(conf)
    h11_values_conf.append(conf12[0])

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

    print "DHU "+str(DHU)
    print "dups "+str(dups)
    print "-- plots host mean time --"
    name_new = []
    for i in range(1,13,1):
        name_new.append("host"+str(i))

    x = np.arange(len(name_new))
    width = 0.5
    figure, axis = plt.subplots(2,1, figsize=(20,10))
    ax=axis[0]
    ax.bar(x, avg_12_no_first, width, label='No first',data=name_new, yerr=conf12)
    ax.set_ylabel('Time')
    ax.set_title('Mean time w/o first ping ('+str(confidence*100)+'% CI)')
    ax.set_xticks(x)
    ax.set_xticklabels(name_new)
    #ax.legend()

    ax=axis[1]
    ax.set_ylabel('Time')
    ax.set_title('First ping')
    ax.set_xticks(x)
    ax.set_xticklabels(name_new)
    #ax.legend()
    #ax.set_yticks(np.arange(0,30,5))
    ax.bar(x, avg_12_complete, width, label='Complete',data=name_new)
    
    
    
    print "-- plots h11 mean time --"
    #colors1=['royalblue','orange','tomato','limegreen']
    colors2=['darkblue','darkorange','firebrick','darkgreen']
    x=[1,4,8,12]
    #x = np.arange(len(name_new2))
    width = 1
    plt.figure(1)
    figure, axis = plt.subplots(2,1, figsize=(20,10))
    ax=axis[0]
    ax.bar(x, h11_values_no_first,width,color=colors2, yerr=h11_values_conf)
    ax.set_ylabel('Time')
    ax.set_title('h11 mean time with differente numbers of hosts pinging ('+str(confidence*100)+'% CI)')
    ax.set_xticks(x)
    #ax.set_xticklabels(name_new2)

    ax=axis[1]
    ax.bar(x, values_complete,width,color=colors2, yerr=conf_complete)
    ax.set_ylabel('Time')
    if FIRST_PING_ONLY_H11:
        ax.set_title('h11 1st ping time with differente numbers of hosts pinging')
    else:
        ax.set_title('1st ping average time with differente numbers of hosts pinging')
    ax.set_xticks(x)
    #ax.set_xticklabels(name_new2)
    plt.show()
