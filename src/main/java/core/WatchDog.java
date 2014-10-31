package core;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import shared.MsgEvent;
import shared.MsgEventType;


public class WatchDog {

	  private Timer timer;
	  private long startTS;
	  private Map<String,String> wdMap;
		
	  
	  public WatchDog() {
		  startTS = System.currentTimeMillis();
		  timer = new Timer();
	      timer.scheduleAtFixedRate(new WatchDogTask(), 500, AgentEngine.config.getWatchDogTimer());
	      wdMap = new HashMap<String,String>(); //for sending future WD messages
	      
	      AgentEngine.watchDogActive = true;
	      MsgEvent le = new MsgEvent(MsgEventType.INFO,AgentEngine.config.getRegion(),null,null,"WatchDog timer set to " + AgentEngine.config.getWatchDogTimer() + " milliseconds");
	      le.setParam("src_region", AgentEngine.region);
		  le.setParam("src_agent", AgentEngine.agent);
		  le.setParam("dst_region", AgentEngine.region);
		  AgentEngine.clog.log(le);
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
	    	 
	    		MsgEvent le = new MsgEvent(MsgEventType.WATCHDOG,AgentEngine.config.getRegion(),null,null,wdMap);
	    		le.setParam("src_region", AgentEngine.region);
	  		    le.setParam("src_agent", AgentEngine.agent);
	  		    le.setParam("dst_region", AgentEngine.region);
	  		    AgentEngine.clog.log(le);
	    	}
	    }
	  }
}
