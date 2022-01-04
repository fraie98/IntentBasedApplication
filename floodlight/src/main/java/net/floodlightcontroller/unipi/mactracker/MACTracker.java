package net.floodlightcontroller.unipi.mactracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.util.HexString;
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
import net.floodlightcontroller.packet.Ethernet;

public class MACTracker implements IOFMessageListener, IFloodlightModule {
	protected IFloodlightProviderService floodlightProvider;
	protected static Logger log;
	
	@Override
	public String getName() {
		return MACTracker.class.getSimpleName();
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

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		Long sourceMACHash = Ethernet.toLong(eth.getSourceMACAddress().getBytes());
		System.out.printf("MAC Address: {%s} seen on switch: {%s}\n",
				HexString.toHexString(sourceMACHash), sw.getId());
		
		int hubBehaviour = 1; 	// Set to 0 for "generate flow-mod"
		
		if(hubBehaviour==1) {
			// Hub Behavior
			OFPacketIn pi = (OFPacketIn) msg;
			
			OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
			pob.setBufferId(pi.getBufferId());
			
			OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
			actionBuilder.setPort(OFPort.FLOOD);
			
			pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
			
			if(pi.getBufferId() == OFBufferId.NO_BUFFER) {
				byte[] packetData = pi.getData();
				pob.setData(packetData);
			}
			
			sw.write(pob.build());
		}
		else if(hubBehaviour==0) {		
			// Generate Flow-Mod
			OFPacketIn pi = (OFPacketIn) msg;
			OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();
			fmb.setBufferId(pi.getBufferId())
				.setHardTimeout(20)
				.setIdleTimeout(10)
				.setPriority(32768)
				.setXid(pi.getXid());
			
			OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
			actionBuilder.setPort(OFPort.FLOOD);
			fmb.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
			
			sw.write(fmb.build());
		}
		
		return Command.STOP;
	}

}
