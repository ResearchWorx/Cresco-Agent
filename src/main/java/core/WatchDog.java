package core;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import channels.RPCCall;
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
	      
	      MsgEvent le = new MsgEvent(MsgEvent.Type.CONFIG,AgentEngine.region,null,null,"enabled");
		  le.setParam("src_region", AgentEngine.region);
		  le.setParam("src_agent", AgentEngine.agent);
		  le.setParam("dst_region", AgentEngine.region);
		  le.setParam("is_active", Boolean.TRUE.toString());
		  le.setParam("watchdog_rate",String.valueOf(AgentEngine.config.getWatchDogTimer()));
		  AgentEngine.msgInQueue.offer(le);
          AgentEngine.watchDogActive = true;
      }

      public void shutdown(boolean unregister) {
          if(!AgentEngine.isRegionalController && unregister) {
              MsgEvent le = new MsgEvent(MsgEvent.Type.CONFIG, AgentEngine.region, null, null, "disabled");
              le.setParam("src_region", AgentEngine.region);
              le.setParam("src_agent", AgentEngine.agent);
              le.setParam("dst_region", AgentEngine.region);
              le.setParam("is_active", Boolean.FALSE.toString());
              le.setParam("watchdog_rate", String.valueOf(AgentEngine.config.getWatchDogTimer()));
              MsgEvent re = new RPCCall().call(le);
              System.out.println("RPC DISABLE: " + re.getMsgBody() + " [" + re.getParams().toString() + "]");
          }
          timer.cancel();
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
	    	 
	    		MsgEvent le = new MsgEvent(MsgEvent.Type.WATCHDOG,AgentEngine.region,null,null,wdMap);
	    		le.setParam("src_region", AgentEngine.region);
	  		    le.setParam("src_agent", AgentEngine.agent);
	  		    le.setParam("dst_region", AgentEngine.region);
	  		    //AgentEngine.clog.log(le);
				AgentEngine.msgInQueue.offer(le);
	    	}
	    }
	  }
}
