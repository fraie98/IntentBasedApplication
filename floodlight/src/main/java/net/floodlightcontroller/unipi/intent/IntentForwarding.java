package net.floodlightcontroller.unipi.intent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;

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
import net.floodlightcontroller.routing.IGatewayService;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingDecisionChangedListener;
import net.floodlightcontroller.routing.Path;



public class IntentForwarding extends Forwarding  implements IFloodlightModule, IOFSwitchListener, ILinkDiscoveryListener,
IRoutingDecisionChangedListener, IGatewayService, IIntentForwarding{
	
	IPv4Address deniedSRC=IPv4Address.of("10.0.0.1");
	IPv4Address deniedDST=IPv4Address.of("10.0.0.2");
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

	//@Override
	//public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO
		/*Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		return l;*/
	//}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		intentsDB = new ArrayList<>();
	}
	
	@Override
    public Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx) {
		if(decision!=null) {
			System.out.printf("decision: %s \n",
					decision.getRoutingAction().toString());  
			return super.processPacketInMessage(sw, pi, decision, cntx);
		}
		IRoutingDecision d=null;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		IPacket pkt = eth.getPayload();
		
		if(eth.isBroadcast() || eth.isMulticast()) {
			return super.processPacketInMessage(sw, pi, d, cntx);
		}
		if(pkt instanceof ARP) {
			return super.processPacketInMessage(sw, pi, decision, cntx);
			
		}
		IPv4 ip_pkt = (IPv4) pkt;
		IPv4Address sourceIP=ip_pkt.getSourceAddress();
		IPv4Address destinIP=ip_pkt.getDestinationAddress();
			
		if(deniedDST.equals(sourceIP) && deniedSRC.equals(destinIP)) {
				System.out.printf("dening: %s - %s on switch %s \n",
						sourceIP.toString(), destinIP.toString(), sw.getId());  
				denyRoute(sw, sourceIP, destinIP,200);
				denyRoute(sw, destinIP, sourceIP,200);
				return Command.CONTINUE;
			}
		System.out.printf("allowing: %s - %s on switch %s \n",
				sourceIP.toString(), destinIP.toString(), sw.getId());  
		return super.processPacketInMessage(sw, pi, d, cntx);
		
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
