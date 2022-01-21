package net.floodlightcontroller.unipi.intent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
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
import net.floodlightcontroller.routing.IRoutingService;

public class IntentNetworkController implements IFloodlightModule, IOFMessageListener   {

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger log;
	protected IRoutingService routingManager;
	public String getName() {
		return IntentNetworkController.class.getSimpleName();
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
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
		log = LoggerFactory.getLogger(IntentNetworkController.class);
		routingManager=context.getServiceImpl(IRoutingService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
	}
;
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

	IPv4Address deniedSRC=IPv4Address.of("10.0.0.1");
	IPv4Address deniedDST=IPv4Address.of("10.0.0.2");
	
	
    protected Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi,  FloodlightContext cntx) {
		
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		IPacket pkt = eth.getPayload();
		
		if(eth.isBroadcast() || eth.isMulticast()) {
			return Command.CONTINUE;
		}
		if(pkt instanceof ARP) {
			return Command.CONTINUE;
			
		}
		IPv4 ip_pkt = (IPv4) pkt;
		IPv4Address sourceIP=ip_pkt.getSourceAddress();
		IPv4Address destinIP=ip_pkt.getDestinationAddress();
			
		if(deniedDST.equals(sourceIP) && deniedSRC.equals(destinIP)
				||deniedDST.equals(destinIP) && deniedSRC.equals(sourceIP) ) {
				System.out.printf("dening: %s - %s on switch %s \n",
						sourceIP.toString(), destinIP.toString(), sw.getId());  
				denyRoute(sw, sourceIP, destinIP,200);
				denyRoute(sw, destinIP, sourceIP,200);
				return Command.STOP;
			}
		System.out.printf("allowing: %s - %s on switch %s \n",
				sourceIP.toString(), destinIP.toString(), sw.getId());  
		return Command.CONTINUE;
		
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
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		if(!msg.getType().equals(OFType.PACKET_IN)) {
			return Command.CONTINUE;
		}
		OFPacketIn pi = (OFPacketIn) msg;
		
		return this.processPacketInMessage(sw, pi, cntx);
	}



}
