package net.floodlightcontroller.unipi.intent;

import java.util.*;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
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
import net.floodlightcontroller.routing.ForwardingBase;
import net.floodlightcontroller.routing.IGatewayService;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingDecisionChangedListener;




public class IntentForwarding extends Forwarding  implements IFloodlightModule, IOFSwitchListener, ILinkDiscoveryListener,
IRoutingDecisionChangedListener, IGatewayService, IIntentForwarding{
	
	private final int DEFAULT_TIMEOUT=5;
	private final int INTENT_PRIORITY=2;
	private final int DENY_PRIORITY=1;
	protected int denyTimeout;
	protected static final Logger log = LoggerFactory.getLogger(IntentForwarding.class);
	ArrayList<HostPair> intentsDB;
	ArrayList<TimeoutTask> timersDB;
	
	public int getDenyTimeout() {
		return denyTimeout;
	}

	public void setDenyTimeout(int denyTimeout) {
		this.denyTimeout = denyTimeout;
	}
	
	@Override
	public String getName() {
		return IntentForwarding.class.getSimpleName();
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				super.getModuleServices();
		l.add(IIntentForwarding.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
				super.getServiceImpls();
		m.put(IIntentForwarding.class, this);
		return m;
	}


	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		intentsDB = new ArrayList<>();
		timersDB = new ArrayList<>();
		denyTimeout = DEFAULT_TIMEOUT;
		ForwardingBase.FLOWMOD_DEFAULT_PRIORITY = INTENT_PRIORITY;
		//log.info("{}",log.isTraceEnabled());
		super.init(context);
	}
	
	@Override
	public void startUp(FloodlightModuleContext context) {
		super.startUp(context);
	    restApiService.addRestletRoutable(new IntentWebRoutable());
	}
	
	private Command handleARP(IOFSwitch sw, OFPacketIn pi, IRoutingDecision d, FloodlightContext cntx) {
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
		MacAddress targetMAC = arp.getTargetHardwareAddress();	// for gratuitous arp or gossip resolving
		MacAddress senderMAC = arp.getSenderHardwareAddress();
		log.info("processing ARP: {} - {} on switch " + sw.getId().toString(),
				senderMAC.toString(), targetMAC.toString());
		if(!targetMAC.equals(MacAddress.of("00:00:00:00:00:00"))) {
			denyArp(sw, senderMAC, targetMAC);
			denyArp(sw, targetMAC, senderMAC);
			log.info("dening ARP: {} - {} on switch " + sw.getId().toString(),
					senderMAC.toString(), targetMAC.toString());
		}
		//denyRoute(sw, senderIP, targetIP);
		//denyRoute(sw, targetIP,senderIP);
		return Command.STOP;
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
			return super.processPacketInMessage(sw, pi, decision, cntx);
		}
		if(!(pkt instanceof IPv4))
			return super.processPacketInMessage(sw, pi, decision, cntx);
		
		IPv4 ip_pkt = (IPv4) pkt;
		IPv4Address sourceIP = ip_pkt.getSourceAddress();
		IPv4Address destinIP = ip_pkt.getDestinationAddress();
		
		HostPair hp = null;
		int hpIndex = intentsDB.indexOf(new HostPair(sourceIP, destinIP));
		if (hpIndex != -1)
			hp = intentsDB.get(hpIndex);
		
		if(hp != null && intentsDB.contains(hp)) {
			log.info("allowing: {} - {} on switch "+ sw.getId().toString(),
						sourceIP.toString(), destinIP.toString());
				/* It is necessary to register the switches (the first switch that
				 * the packet sends by the sender encounters and the first switch
				 * that the response encounters) to the intent DB so that the
				 * timer handler will be able to deny the route when the intent expires.
				 * Notice that only the first switch encountered will be set because
				 * setSw permits the set only if the switch has not been set previously*/	
				if(hp.getHost1IP().equals(sourceIP))
					hp.setSw1(sw);
				if(hp.getHost2IP().equals(sourceIP))
					hp.setSw2(sw);
				return super.processPacketInMessage(sw, pi, decision, cntx);	
		}
		denyRoute(sw, sourceIP, destinIP);
		denyRoute(sw, destinIP,sourceIP);
		return Command.CONTINUE;
		
	}

	private boolean denyRoute(IOFSwitch sw, IPv4Address sourceIP, IPv4Address destinIP) {
		log.info("dening IPv4: {} - {} on switch "+sw.getId().toString(),
				sourceIP.toString(), destinIP.toString());  
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowModify();
		List<OFAction> actions = new ArrayList<OFAction>(); // no actions = drop
		Match.Builder mb1 = sw.getOFFactory().buildMatch();
		
		mb1.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.setExact(MatchField.IPV4_SRC, sourceIP)
				.setExact(MatchField.IPV4_DST, destinIP);
		
		fmb.setActions(actions)
			.setMatch(mb1.build())
			.setHardTimeout(denyTimeout)
			.setPriority(DENY_PRIORITY);
		sw.write(fmb.build());
		return true;
	}
	
	private boolean denyArp(IOFSwitch sw, MacAddress sourceMAC, MacAddress destinMAC) {
		log.info("dening ARP: {} - {} on switch " + sw.getId().toString(),
				sourceMAC.toString(), destinMAC.toString());  
		OFFlowMod.Builder fmb =sw.getOFFactory().buildFlowModify();
		List<OFAction> actions = new ArrayList<OFAction>(); // no actions = drop
		Match.Builder mb1 = sw.getOFFactory().buildMatch();
		
		mb1.setExact(MatchField.ETH_TYPE, EthType.ARP)
				.setExact(MatchField.ARP_SHA, sourceMAC)
				.setExact(MatchField.ARP_THA, destinMAC)
							;
		fmb.setActions(actions)
			.setMatch(mb1.build())
			.setHardTimeout(denyTimeout)
			.setPriority(DENY_PRIORITY);
		sw.write(fmb.build());
		return true;
	}
	
	
	public boolean addNewIntent(HostPair newPair) {
		
		if(intentsDB.contains(newPair)) {
			log.info(" Intent already present in Intents List");
			return false;
		}
	
		long timeout = newPair.getTimeout();
		Timer timer = new Timer();
		TimerTask task = new TimeoutTask(newPair, this);
		timer.schedule(task, timeout);
		intentsDB.add(newPair);
		timersDB.add((TimeoutTask) task);
		log.info("Intent {} added", newPair);
		return true;
	}
	
	public boolean delAllIntents() {
		/* Notice that I am deleting elements from an array while I am cycle
		 * through it (a for or a foreach will not work).
		 * For this reason I delete always the first element until there is 
		 * an element in the array */
		while(intentsDB.size()!=0) {
			boolean ret = delIntent(intentsDB.get(0));
			if(!ret)
				return false;
		}
		log.info("All intents deleted");
		return true;
	}
	
	public boolean delIntent(HostPair toDelete) {
		log.debug("delIntent Called");
		for (TimeoutTask t:timersDB) {
			if(t.targetIntent.equals(toDelete))
				t.cancel();
		}
		if(!intentsDB.remove(toDelete))
			return false;
		
		if(toDelete.getSw1() != null) {
      		log.debug("Installing rule for denying IPv4 on switch %s", toDelete.getSw1().getId());
      		denyRoute(toDelete.getSw1(), toDelete.getHost1IP(), toDelete.getHost2IP());
          	denyRoute(toDelete.getSw1(), toDelete.getHost2IP(), toDelete.getHost1IP());
      	}
      	if(toDelete.getSw2() != null) {
      		log.debug(" Installing rule for denying IPv4 on switch %s", toDelete.getSw2().getId());
      		denyRoute(toDelete.getSw2(), toDelete.getHost1IP(), toDelete.getHost2IP());
          	denyRoute(toDelete.getSw2(), toDelete.getHost2IP(), toDelete.getHost1IP());
      	}
      	log.info("Intent {} deleted", toDelete);
		return true;
	}

	@Override
	public Iterable<HostPair> getIntents() {
		return intentsDB;
	}

}

