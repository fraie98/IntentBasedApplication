package net.floodlightcontroller.unipi.intent;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;


public class HostPair {

	protected MacAddress host1;
	protected IPv4Address host1IP;
	protected DatapathId sw1;
	protected MacAddress host2;
	protected IPv4Address host2IP;
	protected DatapathId sw2;
	protected long timeout;
	protected TimeoutTask timeoutTask;
	
	public MacAddress getHost1() {
		return host1;
	}

	public IPv4Address getHost1IP() {
		return host1IP;
	}

	public DatapathId getSw1() {
		return sw1;
	}

	public MacAddress getHost2() {
		return host2;
	}

	public IPv4Address getHost2IP() {
		return host2IP;
	}

	public void setHost1IP(IPv4Address host1ip) {
		host1IP = host1ip;
	}

	public void setHost2IP(IPv4Address host2ip) {
		host2IP = host2ip;
	}

	public DatapathId getSw2() {
		return sw2;
	}

	public long getTimeout() {
		return timeout;
	}
	
	public void setTimeout(long newTimeout) {
		timeout = newTimeout;
	}
	
	public void setTimeoutTask(TimeoutTask newTimeoutTask) {
		timeoutTask = newTimeoutTask;
	}
	
	public TimeoutTask getTimeoutTask() {
		return timeoutTask;
	}
	
	public HostPair(MacAddress hostA, MacAddress hostB, long timeoutToSet) {
		host1 = hostA;
		host2 = hostB;
		timeout = timeoutToSet;
		sw1 = null;
		sw2 = null;
		host1IP=null;
		host2IP=null;
	}
	
	public HostPair(MacAddress hostA, MacAddress hostB) {
		this(hostA, hostB, 0);
	}

	public HostPair(String hostA, String hostB, long timeoutToSet) {
		this(MacAddress.of(hostA), MacAddress.of(hostB), timeoutToSet );
	}
	public HostPair(IPv4Address hostA, IPv4Address hostB) {
		this(hostA, hostB, 0);
	}
	public HostPair(IPv4Address hostA, IPv4Address hostB, long timeoutToSet) {
		this((MacAddress)null,(MacAddress)null,timeoutToSet);
		host1IP=hostA;
		host2IP=hostB;
	}
	
	public HostPair(String hostA, String hostB, long timeoutToSet, boolean isMac) {
		if (isMac) {
			host1 = MacAddress.of(hostA);
			host2 = MacAddress.of(hostB);
			timeout = timeoutToSet;
			sw1 = null;
			sw2 = null;
			host1IP=null;
			host2IP=null;
		}
		else {
			host1 = (MacAddress)null;
			host2 = (MacAddress)null;
			timeout = timeoutToSet;
			sw1 = null;
			sw2 = null;
			host1IP = IPv4Address.of(hostA); 
			host2IP = IPv4Address.of(hostB);
		}
	}
	
	@Override
	public boolean equals(Object c) {
		if (this == c)
            return true;
        if (c == null)
            return false;
        if (getClass() != c.getClass())
            return false;
		HostPair cm = (HostPair) c;
		// compare IPss
		
		if(cm.host1IP!=null && cm.host2IP!=null && host1IP!=null && host2IP!=null) { 
			if(cm.host1IP.equals(host1IP) && cm.host2IP.equals(host2IP))
				return true;
			if(cm.host1IP.equals(host2IP) && cm.host2IP.equals(host1IP))
				return true; 
			return false;
		}
		// compare MACs
		if(cm.host1==null || cm.host2==null || this.host1==null || this.host2==null)
			return false;
		if(cm.host1.equals(this.host1) && cm.host2.equals(this.host2))
			return true;
		if(cm.host1.equals(this.host2) && cm.host2.equals(this.host1))
			return true;
		return false;
	}
}
