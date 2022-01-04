package net.floodlightcontroller.unipi.loadbalancer;

import java.util.Map;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ILoadBalancerREST extends IFloodlightService {
	public Map<String, Object> getServersInfo();
	public void setHardTimeout(int newValue);
}
