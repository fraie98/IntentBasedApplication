package net.floodlightcontroller.unipi.intent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.forwarding.Forwarding;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.IGatewayService;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingDecisionChangedListener;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.util.OFMessageUtils;



public class IntentForwarding extends Forwarding  implements IFloodlightModule, IOFSwitchListener, ILinkDiscoveryListener,
IRoutingDecisionChangedListener, IGatewayService, IIntentForwarding{
	
	ArrayList<HostPair> intentsDB;
	
	@Override
	public String getName() {
		return IntentForwarding.class.getSimpleName();
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
		//l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		intentsDB = new ArrayList<>();
		super.init(context);
	}
	
	public Command handleARP(IOFSwitch sw, OFPacketIn pi, IRoutingDecision d, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		IPacket pkt = eth.getPayload();
		if(!(pkt instanceof ARP)) {
			log.info("{} not ARP packet", pi.toString());
			return Command.STOP;
		}
		ARP arp = (ARP) pkt;
		IPv4Address senderIP = arp.getSenderProtocolAddress();
		IPv4Address targetIP = arp.getTargetProtocolAddress();
		HostPair hp = new HostPair(senderIP, targetIP);
		if(intentsDB.contains(hp))
			return super.processPacketInMessage(sw, pi, d, cntx);
		MacAddress targetMAC = arp.getTargetHardwareAddress();
		MacAddress senderMAC = arp.getSenderHardwareAddress();
		hp = new HostPair(senderMAC, targetMAC);
		if(intentsDB.contains(hp))
			return super.processPacketInMessage(sw, pi, d, cntx);
		log.info("dening ARP: {} - {} on switch "+sw.getId().toString()+"\n",
				senderMAC.toString(), targetMAC.toString());  
		denyArp(sw, senderMAC, targetMAC,200);
		denyArp(sw, targetMAC, senderMAC,200);
		return Command.CONTINUE;
		
	}
	@Override
    public Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		//OFPort inPort = OFMessageUtils.getInPort(pi);
		IPacket pkt = eth.getPayload();
		
		if(eth.isBroadcast() || eth.isMulticast()) {
			if(pkt instanceof ARP) {
				return handleARP(sw, pi,decision, cntx);
			}
		}
		if(!(pkt instanceof IPv4))
			return super.processPacketInMessage(sw, pi, decision, cntx);
		IPv4 ip_pkt = (IPv4) pkt;
		IPv4Address sourceIP=ip_pkt.getSourceAddress();
		IPv4Address destinIP=ip_pkt.getDestinationAddress();
			
		System.out.printf("allowing: %s - %s on switch %s \n",
				sourceIP.toString(), destinIP.toString(), sw.getId());  
		return super.processPacketInMessage(sw, pi, decision, cntx);
		
	}
	
	private boolean denyRoute(IOFSwitch sw, IPv4Address sourceIP, IPv4Address destinIP, int timeout) {
		OFFlowMod.Builder fmb =sw.getOFFactory().buildFlowAdd();
		List<OFAction> actions = new ArrayList<OFAction>(); // no actions = drop
		Match.Builder mb1 = sw.getOFFactory().buildMatch();
		
		mb1.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.setExact(MatchField.IPV4_SRC, sourceIP)
				.setExact(MatchField.IPV4_DST, destinIP)
							;
		fmb.setActions(actions)
		.setMatch(mb1.build())
		.setHardTimeout(timeout)
		.setPriority(10000);
		sw.write(fmb.build());
		return true;
	}
	
	private boolean denyArp(IOFSwitch sw, MacAddress sourceMAC, MacAddress destinMAC, int timeout) {
		OFFlowMod.Builder fmb =sw.getOFFactory().buildFlowAdd();
		List<OFAction> actions = new ArrayList<OFAction>(); // no actions = drop
		Match.Builder mb1 = sw.getOFFactory().buildMatch();
		
		mb1.setExact(MatchField.ETH_TYPE, EthType.ARP)
				.setExact(MatchField.ARP_SHA, sourceMAC)
				.setExact(MatchField.ARP_THA, destinMAC)
							;
		fmb.setActions(actions)
		.setMatch(mb1.build())
		.setHardTimeout(timeout)
		.setPriority(10000);
		sw.write(fmb.build());
		return true;
	}
	
	public boolean addNewIntent(HostPair newPair) {
		System.out.print("AddNewIntent Called");
		intentsDB.add(newPair);
		return true;
	}
	
	public boolean delIntent(HostPair toDelete) {
		System.out.print("AddNewIntent Called");
		return true;
	}
	
	public boolean installBackupPath(Path backupPath) {
		System.out.print("AddNewIntent Called");
		return true;
	}
	
}
