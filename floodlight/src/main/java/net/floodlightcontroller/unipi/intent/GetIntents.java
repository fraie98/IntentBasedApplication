package net.floodlightcontroller.unipi.intent;

import java.util.ArrayList;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GetIntents extends ServerResource {
	
	@Get("json")
	public ArrayList<String> getInts() {
		IIntentForwarding intentForw = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
		Iterable<HostPair> hostPairs = intentForw.getIntents();
		ArrayList<String> jsonListToReturn = new ArrayList<>();
		try {
			ObjectMapper mapper = new ObjectMapper();
			for (HostPair i : hostPairs) {
				ObjectNode pair = mapper.createObjectNode();
				pair.put("timeout", i.getTimeout());
				if(i.getHost1IP() != null)
					pair.put("host1 IPv4", i.getHost1IP().toString());
				if(i.getHost2IP() !=null)
					pair.put("host2 IPv4", i.getHost2IP().toString());
				String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pair);
				jsonListToReturn.add(json);
			}
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
		// return json with the list of intents;
		return jsonListToReturn;
	}
}
