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
		 AgentEngine.commandExec.cmdExec(me);
     }
     catch(Exception ex)
     {
    	 System.out.println("Agent : MsgRoute : Route Failed " + ex.toString());
     }
			
		
	}
}
