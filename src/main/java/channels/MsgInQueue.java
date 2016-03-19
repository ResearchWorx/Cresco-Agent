package channels;

import shared.MsgEvent;
import shared.MsgEventType;
import core.AgentEngine;
import core.CommandExec;


public class MsgInQueue implements Runnable {

	private CommandExec commandExec;
	
	public MsgInQueue()
	{
		commandExec = new CommandExec();
	}
	
	public void run() 
	{

		AgentEngine.MsgInQueueEnabled = true;
		while(AgentEngine.MsgInQueueEnabled)
		{
			try
			{
				if(AgentEngine.MsgInQueueActive) //process queue if active
				{
					synchronized(AgentEngine.msgInQueue) 
					{
			    		while ((!AgentEngine.msgInQueue.isEmpty()) && AgentEngine.MsgInQueueEnabled) 
			    		{
			    			MsgEvent me = AgentEngine.msgInQueue.poll(); //get logevent
			    				new MsgRoute(me).run(); //route messages in new thread
			    		}
					}
				}
				Thread.sleep(100);
			}
			catch(Exception ex)
			{
				System.out.println("Agent : MsgInQueue Error :" + ex.getMessage());
			}
			
		}
		//shutdown was called
		
	}

	
	


}

