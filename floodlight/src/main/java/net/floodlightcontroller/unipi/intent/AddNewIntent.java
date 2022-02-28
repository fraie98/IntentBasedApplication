package net.floodlightcontroller.unipi.intent;

import java.io.IOException;

import org.restlet.resource.Get;
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
		boolean isMac = false;
		
		try {
			JsonNode root = mapper.readTree(json);
			hostA = root.get("host1").asText();
			hostB = root.get("host2").asText();
			timeout = Integer.parseInt(root.get("timeout").asText());
			isMac = root.get("isMac").asBoolean();
		} catch (IOException e) {
			e.printStackTrace();
		}
		newPair = new HostPair(hostA, hostB, timeout, isMac);
	    IIntentForwarding intentForw = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
	    return intentForw.addNewIntent(newPair);
	}

}
