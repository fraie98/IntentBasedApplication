package net.floodlightcontroller.unipi.intent;

import java.util.TimerTask;

import net.floodlightcontroller.core.IOFSwitch;

public class TimeoutTask extends TimerTask {
	HostPair targetIntent;
	IOFSwitch targetSw;
	IntentForwarding f;
	boolean isSwitch;
	
	public TimeoutTask(HostPair intent, IOFSwitch sw, IntentForwarding fw) {
		targetIntent = intent;
		targetSw = sw;
		f = fw;
		isSwitch = true;
	}
	

	public TimeoutTask(HostPair intent, IntentForwarding fw) {
		targetIntent = intent;
		targetSw = null;
		isSwitch = false;
		f = fw;
	}
	
	public void setSwitch(IOFSwitch sw) {
		if(!isSwitch) {
			targetSw = sw;
			isSwitch = true;
		}
	}
	
    public void run() {
        System.out.println( " The intent has expired" );
        if(isSwitch) {
        	f.denyRoute(targetSw, targetIntent.getHost1IP(), targetIntent.getHost2IP(), 1000);
      		f.denyRoute(targetSw, targetIntent.getHost2IP(), targetIntent.getHost1IP(), 1000);
        }
        f.delIntent(targetIntent);
    }
}
