package net.floodlightcontroller.unipi.intent;

import java.io.IOException;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DelIntent extends ServerResource {
	
	@Post("json")
	public boolean delInt(String json) {
		if(json == null) {
			return false;
		}
				
		ObjectMapper mapper = new ObjectMapper();
		HostPair toDelete = null;
		String hostA = new String();
		String hostB = new String();
		
		try {
			JsonNode root = mapper.readTree(json);
			hostA = root.get("host1_IP").asText();
			hostB = root.get("host2_IP").asText();
		} catch (IOException e) {
			e.printStackTrace();
		}
		toDelete = new HostPair(hostA, hostB);
	    IIntentForwarding intentForw = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
	    return intentForw.delIntent(toDelete);
	}
}
