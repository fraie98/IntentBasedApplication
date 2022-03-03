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
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.ForwardingBase;
import net.floodlightcontroller.routing.IGatewayService;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingDecisionChangedListener;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Path;




public class IntentForwarding extends Forwarding  implements IFloodlightModule, IOFSwitchListener, ILinkDiscoveryListener,
IRoutingDecisionChangedListener, IGatewayService, IIntentForwarding{
	private final int DEFAULT_TIMEOUT=5;

	protected int denyTimeout;
	public int getDenyTimeout() {
		return denyTimeout;
	}

	public void setDenyTimeout(int denyTimeout) {
		this.denyTimeout = denyTimeout;
	}
	ArrayList<HostPair> intentsDB;
	IRoutingService routingService;
	ForwardingBase forwardingBase;
	
	@Override
	public String getName() {
		return IntentForwarding.class.getSimpleName();
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IIntentForwarding.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IIntentForwarding.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		l.add(IRoutingService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		intentsDB = new ArrayList<>();
		routingService = context.getServiceImpl(IRoutingService.class);
		denyTimeout=DEFAULT_TIMEOUT;
		super.init(context);
	}
	
	@Override
	public void startUp(FloodlightModuleContext context) {
		super.startUp();
	    restApiService.addRestletRoutable(new IntentWebRoutable());
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
		MacAddress targetMAC = arp.getTargetHardwareAddress();	// for gratuitous arp or gossip resolving
		MacAddress senderMAC = arp.getSenderHardwareAddress();
		log.info("dening ARP: {} - {} on switch " + sw.getId().toString() + "\n",
				senderMAC.toString(), targetMAC.toString());  
		//denyArp(sw, senderMAC, targetMAC,200);
		//denyArp(sw, targetMAC, senderMAC,200);
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
		
		HostPair hp = getIntentFromDB(sourceIP, destinIP);
		if(hp != null && intentsDB.contains(hp)) {
				System.out.printf("allowing: %s - %s on switch %s \n",
						sourceIP.toString(), destinIP.toString(), sw.getId());
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
	
	private HostPair getIntentFromDB(IPv4Address sourceIP, IPv4Address destIP) {
		for (HostPair i : intentsDB) {
			if((new HostPair(sourceIP, destIP)).equals(i)) {
				return i;
			}
		}
		return null;
	}

	public boolean denyRoute(IOFSwitch sw, IPv4Address sourceIP, IPv4Address destinIP) {
		log.info("dening IPv4: {} - {} on switch "+sw.getId().toString()+"\n",
				sourceIP.toString(), destinIP.toString());  
		OFFlowMod.Builder fmb =sw.getOFFactory().buildFlowAdd();
		List<OFAction> actions = new ArrayList<OFAction>(); // no actions = drop
		Match.Builder mb1 = sw.getOFFactory().buildMatch();
		
		mb1.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.setExact(MatchField.IPV4_SRC, sourceIP)
				.setExact(MatchField.IPV4_DST, destinIP)
							;
		fmb.setActions(actions)
		.setMatch(mb1.build())
		.setHardTimeout(denyTimeout)
		.setPriority(10000);
		sw.write(fmb.build());
		return true;
	}
	
	private boolean denyArp(IOFSwitch sw, MacAddress sourceMAC, MacAddress destinMAC, int timeout) {
		log.info("dening ARP: {} - {} on switch " + sw.getId().toString() + "\n",
				sourceMAC.toString(), destinMAC.toString());  
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
		System.out.print("AddNewIntent Called\n");
		if(intentsDB.contains(newPair)) {
			log.info(" Intent already present in Intents List");
			return false;
		}
	
		long timeout = newPair.getTimeout();
		Timer timer = new Timer();
		TimerTask task = new TimeoutTask(newPair, this);
		newPair.setTimeoutTask((TimeoutTask)task);
		timer.schedule(task, timeout);
		
		intentsDB.add(newPair);
		return true;
	}
	
	public boolean delIntent(HostPair toDelete) {
		System.out.print("delIntent Called\n");
		for (Iterator<HostPair> iterator = intentsDB.iterator(); iterator.hasNext(); ) {
            HostPair i = iterator.next();
            if (i == toDelete) {
                iterator.remove();
            }
        }
		/*for (HostPair i : intentsDB) {
			if (i.getHost1() == toDelete.getHost1() && i.getHost2() == toDelete.getHost2()) {
				intentsDB.remove(i);
			}
		}*/
		return true;
	}
	
	public boolean installBackupPath(Path backupPath) {
		System.out.print("AddNewIntent Called");
		// TODO
		return true;
	}

	@Override
	public Iterable<HostPair> getIntents() {
		return intentsDB;
	}
	
}

