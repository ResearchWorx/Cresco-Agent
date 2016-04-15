package core;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.researchworx.cresco.library.messaging.MsgEvent;


public class WatchDog {

	  public Timer timer;
	  private long startTS;
	  private Map<String,String> wdMap;
		
	  
	  public WatchDog() {
		  startTS = System.currentTimeMillis();
		  timer = new Timer();
	      timer.scheduleAtFixedRate(new WatchDogTask(), 500, AgentEngine.config.getWatchDogTimer());
	      wdMap = new HashMap<>(); //for sending future WD messages
	      
	      AgentEngine.watchDogActive = true;
	      MsgEvent le = new MsgEvent(MsgEvent.Type.CONFIG,AgentEngine.config.getRegion(),null,null,"enabled");
	      le.setParam("src_region", AgentEngine.region);
		  le.setParam("src_agent", AgentEngine.agent);
		  le.setParam("dst_region", AgentEngine.region);
		  le.setParam("watchdogmessage","WatchDog timer set to " + AgentEngine.config.getWatchDogTimer() + " milliseconds");
		  //AgentEngine.msgIn(le);
		  //AgentEngine.clog.log(le);
          AgentEngine.msgInQueue.offer(le);
	  }


	class WatchDogTask extends TimerTask 
	{
	    public void run() 
	    {
	    	
	    	if(AgentEngine.watchDogActive)
	    	{
	    		long runTime = System.currentTimeMillis() - startTS;
	    		wdMap.put("runtime", String.valueOf(runTime));
	    		wdMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
	    	 
	    		MsgEvent le = new MsgEvent(MsgEvent.Type.WATCHDOG,AgentEngine.config.getRegion(),null,null,wdMap);
	    		le.setParam("src_region", AgentEngine.region);
	  		    le.setParam("src_agent", AgentEngine.agent);
	  		    le.setParam("dst_region", AgentEngine.region);
	  		    //AgentEngine.clog.log(le);
				AgentEngine.msgInQueue.offer(le);
	    	}
	    }
	  }
}
