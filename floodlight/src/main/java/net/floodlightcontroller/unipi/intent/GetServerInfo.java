package net.floodlightcontroller.unipi.intent;

import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.core.types.JsonObjectWrapper;

public class GetServerInfo extends ServerResource {
	
    @Get("json")
    public JsonObjectWrapper getServiceClassName() {
    	IIntentForwarding inf= (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
    	Map<String, Object> info = new HashMap<String, Object>();
		info.put("NAME",inf.getName());
		return JsonObjectWrapper.of(info);
    }
}