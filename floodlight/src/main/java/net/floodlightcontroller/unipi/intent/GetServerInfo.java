package net.floodlightcontroller.unipi.intent;

import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class GetServerInfo extends ServerResource {
	
    @Get("json")
    public String Test() {
    	IIntentForwarding lb = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
    	return lb.toString();
    }
}