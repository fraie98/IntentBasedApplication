package net.floodlightcontroller.unipi.intent;


import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class DeleteAllIntents extends ServerResource {
	
	@Post("json")
	public boolean deleteAll() {
		IIntentForwarding intentForw = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
	    return intentForw.delAllIntents();
	}
}
