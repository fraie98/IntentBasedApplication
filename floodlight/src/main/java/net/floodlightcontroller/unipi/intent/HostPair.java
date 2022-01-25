package net.floodlightcontroller.unipi.intent;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.MacAddress;

public class HostPair {

	MacAddress host1;
	DatapathId sw1;
	MacAddress host2;
	DatapathId sw2;
	long timeot;
}
