package net.floodlightcontroller.unipi.intent;

import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class GetServerInfo extends ServerResource {
	
    @Get("json")
    public Map<String, Object> Test() {
    	IIntentRest lb = (IIntentRest) getContext().getAttributes().get(IIntentRest.class.getCanonicalName());
    	return lb.getServersInfo();
    }
}