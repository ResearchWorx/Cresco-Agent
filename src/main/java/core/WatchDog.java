package core;

import channels.RPCCall;
import com.researchworx.cresco.library.messaging.MsgEvent;

import javax.xml.bind.DatatypeConverter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class WatchDog {

	  public Timer timer;
	  private long startTS;
	  private Map<String,String> wdMap;

	  private String jsonExport = null;
	  private String watchDogTimerString;
	  
	  public WatchDog() {
		  startTS = System.currentTimeMillis();
		  timer = new Timer();
          watchDogTimerString = AgentEngine.config.getStringParams("general","watchdogtimer");
          if(watchDogTimerString == null) {
              watchDogTimerString = "5000";
          }


	      timer.scheduleAtFixedRate(new WatchDogTask(), 500, Long.parseLong(watchDogTimerString));
	      wdMap = new HashMap<>(); //for sending future WD messages
	      
	      MsgEvent le = new MsgEvent(MsgEvent.Type.CONFIG,AgentEngine.region,null,null,"enabled");
		  le.setParam("src_region", AgentEngine.region);
		  le.setParam("src_agent", AgentEngine.agent);
		  le.setParam("dst_region", AgentEngine.region);
		  //le.setParam("is_active", Boolean.TRUE.toString());
		  le.setParam("action", "enable");
		  le.setParam("watchdogtimer",watchDogTimerString);

          jsonExport = AgentEngine.pluginexport.getPluginExport();
          String compressedString = DatatypeConverter.printBase64Binary(AgentEngine.stringCompress(jsonExport));
          le.setParam("pluginconfigs", compressedString);


          String platform = System.getenv("CRESCO_PLATFORM");
          if(platform == null) {
              platform = AgentEngine.config.getStringParams("general", "platform");
              if(platform == null) {
                  platform = "unknown";
              }
          }
          le.setParam("platform", platform);

          String environment = System.getenv("CRESCO_ENVIRONMENT");
          if(environment == null) {
              environment = AgentEngine.config.getStringParams("general", "environment");
              if(environment == null) {
				  try {
					  environment = System.getProperty("os.name");
				  } catch (Exception ex) {
					  environment = "unknown";
				  }
              }
          }
          le.setParam("environment", environment);


          String location = System.getenv("CRESCO_LOCATION");
          if(location == null) {
			  location = AgentEngine.config.getStringParams("general", "location");

			  if(location == null) {
		  		try {
					location = InetAddress.getLocalHost().getHostName();
				} catch(Exception ex) {
		  			try {
						String osType = System.getProperty("os.name").toLowerCase();
						if(osType.equals("windows")) {
							location = System.getenv("COMPUTERNAME");
						} else if(osType.equals("linux")) {
							location = System.getenv("HOSTNAME");
						}
						} catch(Exception exx) {
		  				//do nothing
					}
				}
			}
		  }
		  if(location == null) {
		  	location = "unknown";
		  }
		  le.setParam("location", location);


          //public String[] aNodeIndexParams = {"platform","environment","location"};

          System.out.println("starting RPC");
          //AgentEngine.msgInQueue.add(le);
		  MsgEvent re = new RPCCall().call(le);
          System.out.println("end RPC");


		  //System.out.println("RPC ENABLE: " + re.getMsgBody() + " [" + re.getParams().toString() + "]");
		  AgentEngine.watchDogActive = true;
      }

      public void shutdown(boolean unregister) {
          if(!AgentEngine.isRegionalController && unregister) {
              MsgEvent le = new MsgEvent(MsgEvent.Type.CONFIG, AgentEngine.region, null, null, "disabled");
              le.setParam("src_region", AgentEngine.region);
              le.setParam("src_agent", AgentEngine.agent);
              le.setParam("dst_region", AgentEngine.region);
              //le.setParam("is_active", Boolean.FALSE.toString());
			  le.setParam("action", "disable");
              le.setParam("watchdogtimer", watchDogTimerString);
			  AgentEngine.msgInQueue.add(le);
              //MsgEvent re = new RPCCall().call(le);
              //System.out.println("RPC DISABLE: " + re.getMsgBody() + " [" + re.getParams().toString() + "]");
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

	  		    String tmpJsonExport = AgentEngine.pluginexport.getPluginExport();
	  		    if(!jsonExport.equals(tmpJsonExport)) {
                    jsonExport = tmpJsonExport;
                    String compressedString = DatatypeConverter.printBase64Binary(AgentEngine.stringCompress(jsonExport));
                    le.setParam("pluginconfigs", compressedString);
                    //System.out.println("AgentEngine : Export Plugins ");
                    System.out.println("JSON: " + jsonExport);
                    System.out.println("NEW: " + tmpJsonExport);

                }

	  		    //AgentEngine.clog.log(le);
				AgentEngine.msgInQueue.add(le);
	    	}
	    }
	  }
}
