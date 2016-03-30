package channels;

import core.AgentEngine;
import shared.MsgEvent;

public class MsgRoute implements Runnable{

	private static MsgEvent me;
	public MsgRoute(MsgEvent me)
	{
		this.me = me;
	}
	public void run()
	{
     try{

		 boolean isValid = true;
		 if(me.getParam("ttl") != null) //loop detection
		 {
			 int ttlCount = Integer.valueOf(me.getParam("ttl"));

			 if(ttlCount > 10)
			 {
				 System.out.println("**Agent : MsgRoute : High Loop Count**");
				 System.out.println("MsgType=" + me.getMsgType().toString());
				 System.out.println("Region=" + me.getMsgRegion() + " Agent=" + me.getMsgAgent() + " plugin=" + me.getMsgPlugin());
				 System.out.println("params=" + me.getParamsString());
				 isValid = false;
			 }

			 ttlCount++;
			 me.setParam("ttl", String.valueOf(ttlCount));
		 }
		 else
		 {
			 me.setParam("ttl", "0");
		 }
		 if(isValid) {
			 AgentEngine.commandExec.cmdExec(me);
		 }
     }
     catch(Exception ex)
     {
    	 System.out.println("Agent : MsgRoute : Route Failed " + ex.toString());
     }
			
		
	}
}
