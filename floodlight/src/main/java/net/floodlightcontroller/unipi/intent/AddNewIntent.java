package net.floodlightcontroller.unipi.intent;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AddNewIntent extends ServerResource {
	 
	@Post("json")
	public boolean addIntent(String json) {
		// Check if the payload is provided
		if(json == null) {
			return false;
		}
				
		// Parse the JSON input
		ObjectMapper mapper = new ObjectMapper();
		HostPair newPair = null;
		String hostA = new String();
		String hostB = new String();
		long timeout = 0;
		
		try {
			JsonNode root = mapper.readTree(json);
			hostA = root.get("host1_IP").asText();
			hostB = root.get("host2_IP").asText();
			timeout =root.get("timeout").asInt();
		} catch (Exception e) {
			e.printStackTrace();
		}
		newPair = new HostPair(hostA, hostB, timeout);
	    IIntentForwarding intentForw = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
	    return intentForw.addNewIntent(newPair);
	}

}
