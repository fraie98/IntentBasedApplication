import matplotlib.pyplot as plt
import numpy as np

DHU=0
dups=0

def calc(N_HOSTS_TO_TEST, HOST_TESTED):
    hostName=HOST_TESTED
    avgWo1=[]
    avgC=[]
    global DHU
    global dups
    # compiute avg without first ping
    for h in range(N_HOSTS_TO_TEST):
        somma=0
        n_ping_contati=0
        first=0
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
                if first==0:
                    first=float(b[1])
                    continue
                somma+=float(b[1])
                n_ping_contati+=1
        if n_ping_contati!=0:
            avgWo1.append(somma/n_ping_contati)
            avgC.append((somma+first)/(n_ping_contati+1))

    return avgWo1, avgC

if __name__ == "__main__":

    global DHU
    global dups
    hosts=["h11","h12","h13","h14","h21","h22","h23","h24","h31","h32","h33","h34"]
    h11_values_no_first=[]
    h11_values_complete=[]

    avg_1_no_first, avg_1_complete=calc(1,hosts)

    h11_values_no_first.append(avg_1_no_first)
    h11_values_complete.append(avg_1_complete)


    avg_4_no_first, avg_4_complete=calc(4,hosts)
    h11_values_no_first.append(avg_4_no_first[0])
    h11_values_complete.append(avg_4_complete[0])

    avg_8_no_first, avg_8_complete=calc(8,hosts)
    h11_values_no_first.append(avg_8_no_first[0])
    h11_values_complete.append(avg_8_complete[0])


    avg_12_no_first, avg_12_complete=calc(12,hosts)
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

    print "DHU "+str(DHU)
    print "dups "+str(dups)
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
