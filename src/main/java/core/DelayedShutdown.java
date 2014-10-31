package core;

import java.util.Timer;
import java.util.TimerTask;


public class DelayedShutdown {

	  private Timer timer;
	  
	  
	  public DelayedShutdown(long delay) {
		  timer = new Timer();
	      timer.scheduleAtFixedRate(new ShutdownTasks(), delay, delay);
	      String msg = "Agent=" + AgentEngine.config.getAgentName() + "Time-delayed Shutdown Started in " + delay + " ms";
	       AgentEngine.clog.log(msg);
	  }


	class ShutdownTasks extends TimerTask {
	    public void run() 
	    {
	    	System.out.println("Shutting Down");
	    	System.exit(0);
	    }
	  }

}
