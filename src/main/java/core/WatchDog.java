package core;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import shared.logEvent;


public class WatchDog {

	  private Timer timer;
	  public Queue<logEvent> log;
	  
	  public WatchDog(Queue<logEvent> log) {
		  this.log = log;
		  timer = new Timer();
	      timer.scheduleAtFixedRate(new WatchDogTask(), 500, agentEngine.config.getWatchDogTimer());
	      
	      agentEngine.watchDogActive = true;
	      logEvent le = new logEvent("INFO","WatchDog timer set to " + agentEngine.config.getWatchDogTimer() + " milliseconds");
		  log.offer(le);
	  }


	class WatchDogTask extends TimerTask {
	    public void run() {
	    	
	    if(agentEngine.watchDogActive)
	    {
	    	 long runTime = System.currentTimeMillis() - agentEngine.startTS;
			 logEvent le = new logEvent("WATCHDOG","Agent Core Uptime " + String.valueOf(runTime) + "ms");
			 log.offer(le);
	      //timer.cancel(); //Not necessary because we call System.exit
	      //System.exit(0); //Stops the AWT thread (and everything else)
	    }
	    }
	  }

}
