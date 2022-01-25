package net.floodlightcontroller.unipi.intent;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.MacAddress;

public class HostPair {

	MacAddress host1;
	DatapathId sw1;
	MacAddress host2;
	DatapathId sw2;
	long timeout;
	
	public HostPair(String hostA, String hostB, long timeoutToSet) {
		host1 = MacAddress.of(hostA);
		host2 = MacAddress.of(hostB);
		timeout = timeoutToSet;
		sw1 = null;
		sw2 = null;
	}
}
