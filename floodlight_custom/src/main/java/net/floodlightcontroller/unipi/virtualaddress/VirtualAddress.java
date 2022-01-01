package net.floodlightcontroller.unipi.virtualaddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.unipi.mactracker.MACTracker;

public class VirtualAddress implements IFloodlightModule, IOFMessageListener {
	protected IFloodlightProviderService floodlightProvider;
	protected static Logger log;
	private final static MacAddress VIRTUAL_MAC = MacAddress.of("00:00:00:00:00:FE");
	protected IPv4Address VIRTUAL_IP = IPv4Address.of("8.8.8.8");
	
	@Override
	public String getName() {
		return VirtualAddress.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void handleARPRequests(IOFSwitch sw, OFMessage msg, FloodlightContext cntx, OFPacketIn pi) {
		// Double check that the payload is ARP
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
		IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		if (!(eth.getPayload() instanceof ARP))
			return;
		
		// Cast the ARP request
		ARP arpRequest = (ARP) eth.getPayload();

		// Generate ARP reply
		IPacket arpReply = new Ethernet()
				.setSourceMACAddress(VIRTUAL_MAC)
				.setDestinationMACAddress(eth.getSourceMACAddress())
				.setEtherType(EthType.ARP)
				.setPriorityCode(eth.getPriorityCode())
				.setPayload(
						new ARP()
						.setHardwareType(ARP.HW_TYPE_ETHERNET)
						.setProtocolType(ARP.PROTO_TYPE_IP)
						.setHardwareAddressLength((byte) 6)
						.setProtocolAddressLength((byte) 4)
						.setOpCode(ARP.OP_REPLY)
						.setSenderHardwareAddress(VIRTUAL_MAC) // Set my MAC address
						.setSenderProtocolAddress(VIRTUAL_IP) // Set my IP address
						.setTargetHardwareAddress(arpRequest.getSenderHardwareAddress())
						.setTargetProtocolAddress(arpRequest.getSenderProtocolAddress())
		);
		
		// Initialize a packet out
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		
		pob.setBufferId(OFBufferId.NO_BUFFER);
		pob.setInPort(OFPort.ANY);
		
		// Set the output action
		OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
		OFPort inPort = pi.getMatch().get(MatchField.IN_PORT);
		actionBuilder.setPort(inPort);
		
		pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
		
		// Set the ARP reply as packet data
		byte[] packetData = arpReply.serialize();
		pob.setData(packetData);
		
		System.out.printf(" Send arp reply from %s to %s\n", VIRTUAL_MAC.toString(), eth.getSourceMACAddress().toString());
		
		// Send packet
		sw.write(pob.build());
	}
	
	private void handleIPPacket(IOFSwitch sw, OFMessage msg, FloodlightContext cntx, OFPacketIn pi) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		// Cast the IP packet
		IPv4 ipv4 = (IPv4) eth.getPayload();
		// Check that the IP is actually an ICMP request
		if (! (ipv4.getPayload() instanceof ICMP))
			return;
		// Cast to ICMP packet
		ICMP icmpRequest = (ICMP) ipv4.getPayload();
		
		// Generate ICMP reply
		IPacket icmpReply = new Ethernet()
				.setSourceMACAddress(VIRTUAL_MAC)
				.setDestinationMACAddress(eth.getSourceMACAddress())
				.setEtherType(EthType.IPv4)
				.setPriorityCode(eth.getPriorityCode())
				.setPayload(
						new IPv4()
						.setProtocol(IpProtocol.ICMP)
						.setDestinationAddress(ipv4.getSourceAddress())
						.setSourceAddress(VIRTUAL_IP)
						.setTtl((byte)64)
						.setProtocol(IpProtocol.IPv4)
						// Set the same payload included in the request
						.setPayload(
								new ICMP()
								.setIcmpType(ICMP.ECHO_REPLY)
								.setIcmpCode(icmpRequest.getIcmpCode())
									.setPayload(icmpRequest.getPayload())
								)
						);
		
		// Initialize a packet out
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		
		pob.setBufferId(OFBufferId.NO_BUFFER);
		pob.setInPort(OFPort.ANY);
		
		// Set the output action
		OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
		OFPort inPort = pi.getMatch().get(MatchField.IN_PORT);
		actionBuilder.setPort(inPort);
		
		pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
		
		// Set the ARP reply as packet data
		byte[] packetData = icmpReply.serialize();
		pob.setData(packetData);
		
		System.out.printf(" Send icmp reply from %s to %s\n", VIRTUAL_IP.toString(), ipv4.getSourceAddress().toString());
		// Send packet
		sw.write(pob.build());
	}
	
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		OFPacketIn pi = (OFPacketIn) msg;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		IPacket pkt = eth.getPayload();
		
		if(eth.isBroadcast() || eth.isMulticast()) {
			if(pkt instanceof ARP) {
				handleARPRequests(sw, msg, cntx, pi);
				return Command.STOP;
			}
		}
		else {
			IPv4 ip_pkt = (IPv4) pkt;
			if(ip_pkt.getDestinationAddress().compareTo(VIRTUAL_IP)==0) {
				handleIPPacket(sw, msg, cntx, pi);
				return Command.STOP;
			}
		}
		return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		log = LoggerFactory.getLogger(MACTracker.class); 
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

}
