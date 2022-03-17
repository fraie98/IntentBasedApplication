package net.floodlightcontroller.unipi.intent;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.core.web.ControllerSummaryResource;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;
import net.floodlightcontroller.core.web.LoadedModuleLoaderResource;
import net.floodlightcontroller.restserver.RestletRoutable;


public class IntentWebRoutable implements RestletRoutable {
    /**
     * Create the Restlet router and bind to the proper resources.
     */
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        
        // Add some pre-defined REST resources
        
        // This resource will show the some summary stats on the controller
        router.attach("/controller/summary/json", ControllerSummaryResource.class);
        
        // This resource will show the list of modules loaded in the controller
        router.attach("/module/loaded/json", LoadedModuleLoaderResource.class);
        
        // This resource will show the list of switches connected to the controller	        
        router.attach("/controller/switches/json", ControllerSwitchesResource.class);
        router.attach("/getServersInfo/json", GetServerInfo.class); 
        router.attach("/addNewIntent/json", AddNewIntent.class);
        router.attach("/getIntents/json", GetIntents.class);
        router.attach("/delIntent/json", DelIntent.class);
        router.attach("/deleteAllIntents/json", DeleteAllIntents.class);
        router.attach("/ManageTimeout/json", ManageTimeout.class);
        return router;
    }

    /**
     * Set the base path for the Topology
     */
    @Override
    public String basePath() {
        return "/lb";
    }
}
