package net.floodlightcontroller.unipi.intent;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class getIntents extends ServerResource {
	
	@Get("json")
	public Iterable<HostPair> getInts() {
		IIntentForwarding intentForw = (IIntentForwarding) getContext().getAttributes().get(IIntentForwarding.class.getCanonicalName());
		Iterable<HostPair> hostPairs = intentForw.getIntents();
		String json = new String();
		try {
			ObjectMapper mapper = new ObjectMapper();
			for (HostPair i : hostPairs) {
				ObjectNode pair = mapper.createObjectNode();
				pair.put("host1", i.getHost1().toString());
				pair.put("host2", i.getHost2().toString());
				pair.put("timeout", i.getTimeout());
				json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pair);
			}
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
		// return json;
		return hostPairs;
	}
}
