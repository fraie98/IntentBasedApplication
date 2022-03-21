package net.floodlightcontroller.unipi.intent;


import java.util.TimerTask;

import org.projectfloodlight.openflow.types.IPv4Address;
import net.floodlightcontroller.core.IOFSwitch;


public class HostPair {

	protected IPv4Address host1IP;
	protected IOFSwitch sw1;
	protected IPv4Address host2IP;
	protected IOFSwitch sw2;
	protected long timeout;

	public HostPair(IPv4Address hostA, IPv4Address hostB, long timeoutToSet) {
		host1IP = hostA;
		host2IP = hostB;
		timeout = timeoutToSet;
		sw1 = null;
		sw2 = null;
	}
	
	public HostPair(IPv4Address hostA, IPv4Address hostB) {
		this(hostA, hostB, 0);
	}
	
	public HostPair(String hostA, String hostB, long timeoutToSet) {
		this(IPv4Address.of(hostA), IPv4Address.of(hostB), timeoutToSet);
	}
	
	public HostPair(String hostA, String hostB) {
		this(IPv4Address.of(hostA), IPv4Address.of(hostB), 0);
	}

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
		
		return false;
	}
	
	@Override 
	public String  toString(){
		return host1IP.toString()+" - "+host2IP.toString();
		
	}
	
	
}
