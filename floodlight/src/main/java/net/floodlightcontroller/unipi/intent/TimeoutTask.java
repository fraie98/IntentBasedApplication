package net.floodlightcontroller.unipi.intent;

import java.util.TimerTask;

public class TimeoutTask extends TimerTask {
	HostPair targetIntent;
	IntentForwarding forw;
	
	public TimeoutTask(HostPair intent, IntentForwarding f) {
		targetIntent = intent;
		forw = f;
	}
    public void run() {
        System.out.println( " The intent has expired" );
        
    }
}
