package net.floodlightcontroller.unipi.intent;


import java.io.IOException;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.floodlightcontroller.core.types.JsonObjectWrapper;

public class ManageTimeout extends ServerResource {
	
	@Get("json")
	public JsonObjectWrapper getTimeoutValue() {
		IIntentForwarding intentForw = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
		return JsonObjectWrapper.of(intentForw.getDenyTimeout());
		
	}
	
	@Post("json")
	public boolean setTimeoutValue(String json) {
		IIntentForwarding intentForw = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
		if(json == null) {
			return false;
		}
		ObjectMapper mapper = new ObjectMapper();
		int timeout = 0;
		try {
			JsonNode root = mapper.readTree(json);
			timeout = Integer.parseInt(root.get("timeout").asText());
		} catch (IOException e) {
				e.printStackTrace();
				return false;
		}
		intentForw.setDenyTimeout(timeout);
		return true;
		
	}
	
}
