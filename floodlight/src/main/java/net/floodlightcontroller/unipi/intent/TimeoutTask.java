package net.floodlightcontroller.unipi.intent;

import java.util.TimerTask;

public class TimeoutTask extends TimerTask {
	HostPair targetIntent;
	IntentForwarding f;

	
	public TimeoutTask(HostPair intent, IntentForwarding fw) {
		targetIntent = intent;
		f = fw;
	}
	
    public void run() {
        System.out.println( " The intent has expired" );
      	f.delIntent(targetIntent);
    }
}
