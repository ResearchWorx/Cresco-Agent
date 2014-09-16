package core;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import channels.LogEvent;


public class WatchDog {

	  private Timer timer;
	  public Queue<LogEvent> log;
	  private long startTS;
		
	  
	  public WatchDog(Queue<LogEvent> log) {
		  startTS = System.currentTimeMillis();
		  this.log = log;
		  timer = new Timer();
	      timer.scheduleAtFixedRate(new WatchDogTask(), 500, AgentEngine.config.getWatchDogTimer());
	      
	      AgentEngine.watchDogActive = true;
	      LogEvent le = new LogEvent("INFO","WatchDog timer set to " + AgentEngine.config.getWatchDogTimer() + " milliseconds");
		  log.offer(le);
	  }


	class WatchDogTask extends TimerTask {
	    public void run() {
	    	
	    if(AgentEngine.watchDogActive)
	    {
	    	 long runTime = System.currentTimeMillis() - startTS;
			 LogEvent le = new LogEvent("WATCHDOG","Agent Core Uptime " + String.valueOf(runTime) + "ms");
			 log.offer(le);
	      //timer.cancel(); //Not necessary because we call System.exit
	      //System.exit(0); //Stops the AWT thread (and everything else)
	    }
	    }
	  }

}
