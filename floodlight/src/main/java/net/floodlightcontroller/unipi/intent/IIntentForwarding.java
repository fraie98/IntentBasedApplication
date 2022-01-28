package net.floodlightcontroller.unipi.intent;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.routing.Path;

/**
 * This interface exposes the methods that can be called
 * by REST API and by the LinkFailureHandler.
 * These methods are implemented in IntentForwarding.java
 */
public interface IIntentForwarding extends IFloodlightService{
	public boolean addNewIntent(HostPair newPair);
	public boolean delIntent(HostPair toDelete);
	public boolean installBackupPath(Path backupPath); // called by LinkFailureHandler
	public Iterable<HostPair> getIntents();
	public String getName();
	
}
