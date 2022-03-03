package net.floodlightcontroller.unipi.intent;

import java.util.TimerTask;

import net.floodlightcontroller.core.IOFSwitch;

public class TimeoutTask extends TimerTask {
	HostPair targetIntent;
	IntentForwarding f;

	
	public TimeoutTask(HostPair intent, IntentForwarding fw) {
		targetIntent = intent;
		f = fw;
	}
	
    public void run() {
        System.out.println( " The intent has expired" );
      	if(targetIntent.getSw1() != null) {
      		System.out.printf(" Installing rule for denying IPv4 on switch %s\n", targetIntent.getSw1().getId());
      		f.denyRoute(targetIntent.getSw1(), targetIntent.getHost1IP(), targetIntent.getHost2IP());
          	f.denyRoute(targetIntent.getSw1(), targetIntent.getHost2IP(), targetIntent.getHost1IP());
      	}
      	if(targetIntent.getSw2() != null) {
      		System.out.printf(" Installing rule for denying IPv4 on switch %s\n", targetIntent.getSw2().getId());
      		f.denyRoute(targetIntent.getSw2(), targetIntent.getHost1IP(), targetIntent.getHost2IP());
          	f.denyRoute(targetIntent.getSw2(), targetIntent.getHost2IP(), targetIntent.getHost1IP());
      	}
        f.delIntent(targetIntent);
    }
}
