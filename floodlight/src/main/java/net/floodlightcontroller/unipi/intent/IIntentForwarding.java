package net.floodlightcontroller.unipi.intent;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * This interface exposes the methods that can be called
 * by REST API and by the LinkFailureHandler.
 * These methods are implemented in IntentForwarding.java
 */
public interface IIntentForwarding extends IFloodlightService{
	public boolean addNewIntent(HostPair newPair);
	public boolean delIntent(HostPair toDelete);
	public boolean delAllIntents();
	public Iterable<HostPair> getIntents();
	public String getName();
	public int getDenyTimeout();
	public void setDenyTimeout(int denyTimeout);
	
}
