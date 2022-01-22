package net.floodlightcontroller.unipi.intent;

import java.util.Map;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.SwitchMessagePair;
import net.floodlightcontroller.util.ConcurrentCircularBuffer;

public interface IIntentRest extends IFloodlightService {
	public Map<String, Object> getServersInfo();
}