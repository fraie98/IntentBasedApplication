#!/usr/bin/python2
import matplotlib.pyplot as plt
import numpy as np
import scipy.stats as st

DHU = 0
dups = 0
conf_lev = 0.8
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
    times=[]
    global DHU
    global dups
    # compiute avg without first ping
    for h in range(N_HOSTS_TO_TEST):
        times.append([])
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
                times[h].append(float(b[1]))         
    return times

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
    n_hosts=[1,4,8,12]
    total_val_1st = []
    total_conf_1st =[]
    total_val = []
    total_conf =[]

    single_val_1st = []
    single_val = []
    single_conf =[]
    firsts=[]
    i=0
    for n in n_hosts:
        times=np.array(calc(n,hosts))
        for i in range(0,n):
            if  n==12:
                avg, conf=mean_confidence_interval(times[i,1:], conf_lev)
                single_val.append(avg)
                single_conf.append(conf)
                single_val_1st.append(times[i,0])
        avg, conf=mean_confidence_interval(times[:,0], conf_lev)
        total_val_1st.append(avg)
        total_conf_1st.append(conf)
        t=np.array(times[:,1:]).flatten()
        avg, conf=mean_confidence_interval(t, conf_lev)
        total_val.append(avg)
        total_conf.append(conf)
        

    print "DHU "+str(DHU)
    print "dups "+str(dups)
    print "-- plots host mean time --"

    x = np.arange(len(hosts))
    width = 0.5
    figure, axis = plt.subplots(2,1, figsize=(10,20))
    ax=axis[0]
    ax.bar(x, single_val, width, label='No first',data=hosts, yerr=single_conf)
    ax.set_ylabel('Time')
    ax.set_title('Mean time w/o first ping ('+str(conf_lev*100)+'% CI)')
    ax.set_xticks(x)
    ax.set_xticklabels(hosts)
    #ax.legend()

    ax=axis[1]
    ax.set_ylabel('Time')
    ax.set_title('First ping')
    ax.set_xticks(x)
    ax.set_xticklabels(hosts)
    #ax.legend()
    #ax.set_yticks(np.arange(0,30,5))
    ax.bar(x, single_val_1st, width, label='Complete',data=hosts)
    
    
    
    print "-- plots h11 mean time --"
    #colors1=['royalblue','orange','tomato','limegreen']
    colors2=['darkblue','darkorange','firebrick','darkgreen']
    x=[1,4,8,12]
    #x = np.arange(len(name_new2))
    width = 1.5
    plt.figure(1)
    figure, axis = plt.subplots(2,1, figsize=(10,20))
    ax=axis[0]
    ax.bar(x, total_val,width,color=colors2, yerr=total_conf)
    ax.set_ylabel('Time')
    ax.set_title('Mean time with differente numbers of hosts pinging ('+str(conf_lev*100)+'% CI) w/o first ping')
    ax.set_xticks(x)
    #ax.set_xticklabels(name_new2)

    ax=axis[1]
    ax.bar(x, total_val_1st,width,color=colors2, yerr=total_conf_1st)
    ax.set_ylabel('Time')
    ax.set_title('1st ping mean time with differente numbers of hosts pinging ('+str(conf_lev*100)+'% CI)')
    ax.set_xticks(x)
    #ax.set_xticklabels(name_new2)
    plt.show()
