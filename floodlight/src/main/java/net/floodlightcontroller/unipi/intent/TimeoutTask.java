package net.floodlightcontroller.unipi.intent;

import java.util.TimerTask;

import net.floodlightcontroller.core.IOFSwitch;

public class TimeoutTask extends TimerTask {
	HostPair targetIntent;
	IOFSwitch targetSw;
	IntentForwarding f;
	
	public TimeoutTask(HostPair intent, IOFSwitch sw, IntentForwarding fw) {
		targetIntent = intent;
		targetSw = sw;
		f = fw;
	}
    public void run() {
        System.out.println( " The intent has expired" );
        f.denyRoute(targetSw, targetIntent.getHost1IP(), targetIntent.getHost2IP(), 1000);
      	f.denyRoute(targetSw, targetIntent.getHost2IP(), targetIntent.getHost1IP(), 1000);
        f.delIntent(targetIntent);
    }
}
