package net.floodlightcontroller.unipi.loadbalancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
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
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.unipi.mactracker.MACTracker;
import net.floodlightcontroller.util.FlowModUtils;

public class LoadBalancer implements IOFMessageListener, IFloodlightModule {
	protected IFloodlightProviderService floodlightProvider;
	protected static Logger log;
	static String[] SERVERS_MAC;
	static String[] SERVERS_IP;
	private int[] SERVERS_PORT;
	private int last_server = 0;
	//private int client_port = 1;
	
	private final static MacAddress VIRTUAL_MAC = MacAddress.of("00:00:00:00:00:FE");
	private final static IPv4Address VIRTUAL_IP = IPv4Address.of("8.8.8.8");
	private final static int HARD_TIMEOUT = 20;
	private final static int IDLE_TIMEOUT = 20;
	
	protected IRestApiService restApiService;
	
	@Override
	public String getName() {
		return LoadBalancer.class.getSimpleName();
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
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		log = LoggerFactory.getLogger(MACTracker.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
		SERVERS_MAC = new String[2];
		SERVERS_MAC[0] = "00:00:00:00:00:02";
		SERVERS_MAC[1] = "00:00:00:00:00:03";
		SERVERS_IP = new String[2];
		SERVERS_IP[0] = "10.0.0.2";
		SERVERS_IP[1] = "10.0.0.3";
		SERVERS_PORT = new int[2];
		SERVERS_PORT[0] = 2;
		SERVERS_PORT[1] = 3;
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApiService.addRestletRoutable(new LoadBalancerWebRoutable());
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
	
	private void fromHostToServer(OFFlowMod.Builder fmb, IOFSwitch sw) {
		// Create the match for the new rule (incoming IPv4 traffic directed to the load balancer)
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.setExact(MatchField.IPV4_DST, VIRTUAL_IP)
				.setExact(MatchField.ETH_DST, VIRTUAL_MAC);
				
		// Create the list of actions associated with a match
		OFActions actions = sw.getOFFactory().actions();
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFOxms oxms = sw.getOFFactory().oxms();
				
		// Set as new MAC destination the MAC address of the current server
		OFActionSetField setDlDst = actions.buildSetField()
					.setField(
						oxms.buildEthDst()
							.setValue(MacAddress.of(SERVERS_MAC[last_server]))
							.build()
					)
					.build();
		actionList.add(setDlDst);
				
		// Set as new IP destination the IP address of the current server
		OFActionSetField setNwDst = actions.buildSetField()
				.setField(
						oxms.buildIpv4Dst()
							.setValue(IPv4Address.of(SERVERS_IP[last_server]))
							.build()
						)
				.build();
		actionList.add(setNwDst);
			
		// Set as output port the port of the current server
		OFActionOutput output = actions.buildOutput()
				.setMaxLen(0xFFffFFff)
				.setPort(OFPort.of(SERVERS_PORT[last_server]))
				.build();
		actionList.add(output);
		
		System.out.printf(" From host to server %s\n", SERVERS_IP[last_server]);
		last_server = (last_server+1)%2;
		
		// Send out the mod message
		fmb.setActions(actionList);
		fmb.setMatch(mb.build());
		sw.write(fmb.build());
	}
	
	private void fromServerToHost(OFFlowMod.Builder fmb, IOFSwitch sw) {
		// Create the match for the new rule (incoming IPv4 traffic directed to the load balancer)
				Match.Builder mb1 = sw.getOFFactory().buildMatch();
				mb1.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IPV4_SRC, IPv4Address.of(SERVERS_IP[0]))
						.setExact(MatchField.ETH_SRC, MacAddress.of(SERVERS_MAC[0]));
				Match.Builder mb2 = sw.getOFFactory().buildMatch();
				mb2.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IPV4_SRC, IPv4Address.of(SERVERS_IP[1]))
						.setExact(MatchField.ETH_SRC, MacAddress.of(SERVERS_MAC[1]));
						
				// Create the list of actions associated with a match
				OFActions actions = sw.getOFFactory().actions();
				ArrayList<OFAction> actionList = new ArrayList<OFAction>();
				OFOxms oxms = sw.getOFFactory().oxms();
						
				// Set as new MAC source the MAC address of the virtual server
				OFActionSetField setDlSrc = actions.buildSetField()
							.setField(
								oxms.buildEthSrc()
									.setValue(VIRTUAL_MAC)
									.build()
							)
							.build();
				actionList.add(setDlSrc);
						
				// Set as new IP source the IP address of virtual server
				OFActionSetField setNwSrc = actions.buildSetField()
						.setField(
								oxms.buildIpv4Src()
									.setValue(VIRTUAL_IP)
									.build()
								)
						.build();
				actionList.add(setNwSrc);
					
				// Set as output port the port of the client
				OFActionOutput output = actions.buildOutput()
						.setMaxLen(0xFFffFFff)
						.setPort(OFPort.of(1))
						.build();
				actionList.add(output);
				
				System.out.printf(" From server 1 or 2 transfor to client\n");
				OFFlowMod.Builder fmb2 = sw.getOFFactory().buildFlowAdd();
				fmb2.setIdleTimeout(IDLE_TIMEOUT);
				fmb2.setHardTimeout(HARD_TIMEOUT); // Set as hard timeout the period to change the current server
				fmb2.setBufferId(OFBufferId.NO_BUFFER);
				fmb2.setOutPort(OFPort.CONTROLLER);
				fmb2.setCookie(U64.of(0));
				fmb2.setPriority(FlowModUtils.PRIORITY_MAX);
				
				// Send out the mod message
				fmb.setActions(actionList);
				fmb.setMatch(mb1.build());
				fmb2.setActions(actionList);
				fmb2.setMatch(mb2.build());
				sw.write(fmb.build());
				sw.write(fmb2.build());
	}
	
	private void handleIPPacket(IOFSwitch sw, OFMessage msg, FloodlightContext cntx, OFPacketIn pi) {
		// Create a flow table modification message to add a rule
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
			fmb.setIdleTimeout(IDLE_TIMEOUT);
			fmb.setHardTimeout(HARD_TIMEOUT); // Set as hard timeout the period to change the current server
			fmb.setBufferId(OFBufferId.NO_BUFFER);
			fmb.setOutPort(OFPort.CONTROLLER);
			fmb.setCookie(U64.of(0));
			fmb.setPriority(FlowModUtils.PRIORITY_MAX);
			
		fromHostToServer(fmb, sw);
		fromServerToHost(fmb, sw);
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
			if(pkt instanceof IPv4) {
				IPv4 ip_pkt = (IPv4) pkt;
				if(ip_pkt.getDestinationAddress().compareTo(VIRTUAL_IP)==0) {
					handleIPPacket(sw, msg, cntx, pi);
					return Command.STOP;
				}
			}
			else {
				// ARP response from virtual server to client
				handleARPRequests(sw, msg, cntx, pi);
			}
		}
		return Command.CONTINUE;
	}

}
