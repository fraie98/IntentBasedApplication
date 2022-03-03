package net.floodlightcontroller.unipi.intent;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

import net.floodlightcontroller.core.IOFSwitch;


public class HostPair {

	//protected MacAddress host1;
	protected IPv4Address host1IP;
	protected IOFSwitch sw1;
	//protected MacAddress host2;
	protected IPv4Address host2IP;
	protected IOFSwitch sw2;
	protected long timeout;
	protected TimeoutTask timeoutTask;
	

	public HostPair(IPv4Address hostA, IPv4Address hostB, long timeoutToSet) {
		host1IP = hostA;
		host2IP = hostB;
		//host1 = (MacAddress)null;
		//host2 = (MacAddress)null;
		timeout = timeoutToSet;
		sw1 = null;
		sw2 = null;
		timeoutTask = null;
	}
	
	public HostPair(IPv4Address hostA, IPv4Address hostB) {
		this(hostA, hostB, 0);
	}
	
	public HostPair(String hostA, String hostB, long timeoutToSet) {
		this(IPv4Address.of(hostA), IPv4Address.of(hostB), timeoutToSet);
	}
	
	/*public MacAddress getHost1() {
		return host1;
	}*/
	
	public void setHost1IP(IPv4Address host1ip) {
		host1IP = host1ip;
	}

	public IPv4Address getHost1IP() {
		return host1IP;
	}
	
	public void setSw1(IOFSwitch newSw1) {
		if(sw1 == null)
			sw1 = newSw1;
	}
	
	public IOFSwitch getSw1() {
		return sw1;
	}
	/*
	public MacAddress getHost2() {
		return host2;
	}*/
	
	public void setHost2IP(IPv4Address host2ip) {
		host2IP = host2ip;
	}

	public IPv4Address getHost2IP() {
		return host2IP;
	}	
	
	public void setSw2(IOFSwitch newSw2) {
		if(sw2 == null)
			sw2 = newSw2;
	}
	

	public IOFSwitch getSw2() {
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
	
	/*public HostPair(MacAddress hostA, MacAddress hostB, long timeoutToSet) {
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
	}*/
	
	
	
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
		/*
		if(cm.host1==null || cm.host2==null || this.host1==null || this.host2==null)
			return false;
		if(cm.host1.equals(this.host1) && cm.host2.equals(this.host2))
			return true;
		if(cm.host1.equals(this.host2) && cm.host2.equals(this.host1))
			return true;*/
		return false;
	}
}
