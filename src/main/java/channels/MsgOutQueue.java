package channels;

import core.AgentEngine;


public class MsgOutQueue implements Runnable {

	
	public MsgOutQueue()
	{
	}
	public void run() 
	{

		/*AgentEngine.MsgOutQueueEnabled = true;
		while(AgentEngine.MsgOutQueueEnabled)
		{
			try
			{
				Thread.sleep(1000);
				if(AgentEngine.MsgOutQueueActive) //process queue if active
				{
					
				}
			}
			catch(Exception ex)
			{
				System.out.println("Agent : MsgOutQueue Error :" + ex.getMessage());
			}
			
		}*/
		//shutdown was called
		
	}
}

