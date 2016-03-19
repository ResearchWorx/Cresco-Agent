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
    	 
    	 if(me.getParam("loop") != null) //loop detection
			{
				int loopCount = Integer.valueOf(me.getParam("loop"));
			
				if(loopCount > 10)
				{
					
					System.out.println("**Agent : MsgRoute : High Loop Count**");
	    			System.out.println("MsgType=" + me.getMsgType().toString());
					System.out.println("Region=" + me.getMsgRegion() + " Agent=" + me.getMsgAgent() + " plugin=" + me.getMsgPlugin());
					System.out.println("params=" + me.getParamsString());
					
				}
				
				loopCount++;
				me.setParam("loop", String.valueOf(loopCount));
			}
			else
			{
				me.setParam("loop", "0");
			}
    	 
		//if message is a log message
		if((me.getParam("dst_region") != null) && (me.getParam("dst_agent") == null) && (me.getParam("dst_plugin") == null))
		{
			if(AgentEngine.isActive)
			{
				//if we are a controller
				if(AgentEngine.isRegionalController)
				{		    						
				
					if(AgentEngine.controllerPluginSlot != null) //controller plugin was found 
					{
						me.setMsgAgent(AgentEngine.agent); //route to this agent
						me.setMsgPlugin(AgentEngine.controllerPluginSlot); //route to controller plugin					
						AgentEngine.commandExec.cmdExec(me); 
					}
					else
					{
						System.out.println("Agent : MsgInQueue Info : Agent is Controller but Controller Plugin Not found! Region=" + me.getMsgRegion() + " Agent=" + me.getMsgAgent());	
					}
				}
				else
				{
					//need to route log message to outgoing log
					//process logs anyway
					if(AgentEngine.hasChannel)
					{
						if(AgentEngine.channelPluginSlot != null) //controller plugin was found 
						{
							me.setMsgAgent(AgentEngine.agent); //route to this agent
							me.setMsgPlugin(AgentEngine.channelPluginSlot); //route to controller plugin					
							AgentEngine.commandExec.cmdExec(me);
						}
					}
				}
			}
			else //process log if you have a channel even if not active
			{
				//do nothing
				//System.out.println("Agent : MsgRoute : Can't log!  No active channel!");
			}
		}
		//not a log message
		else if((me.getParam("dst_region") != null) && (me.getParam("dst_agent") != null))
		{
			try
			{
				//this is an agent message
				if((me.getParam("dst_region").equals(AgentEngine.region)) && (me.getParam("dst_agent").equals(AgentEngine.agent)))
				{
					//this is a message for this agent
					AgentEngine.msgIn(me);
					
				}
				else
				{
					if(me.getParam("dst_region").equals(AgentEngine.region)) //msg can be routed to this region
					{
						//agent in our region.. should be reachable need to implement RPC to controller
						//add path via plugin and pluginName
						//send to appropirate outgoing plugin
						if(AgentEngine.channelPluginSlot != null) //controller plugin was found 
						{
							//System.out.println("Agent : MsgInQueue : using plugin=" + AgentEngine.channelPluginSlot + " for regional route." );
							me.setMsgAgent(AgentEngine.agent); //route to this agent
							me.setMsgPlugin(AgentEngine.channelPluginSlot); //route to controller plugin					
							AgentEngine.commandExec.cmdExec(me);
							//System.out.println("Agent : MsgInQueue : using plugin=" + AgentEngine.channelPluginSlot + " for regional route return CODY ." );
							
						}
						else
						{
							System.out.println("Agent : MsgInQueue : No Plugin for regional Message!");						
						}
						
					}
					else
					{
						System.out.println("Agent : MsgInQueue : !MSG For Another AGENT IN ANOTHER REGION!!!");
					}
				}
			}
			catch(Exception ex)
			{
				System.out.println("Agent : MsgRoute : Agent Message Error " + ex.toString());
				System.out.println("MsgType=" + me.getMsgType().toString());
				System.out.println("Region=" + me.getMsgRegion() + " Agent=" + me.getMsgAgent() + " plugin=" + me.getMsgPlugin());
				System.out.println("params=" + me.getParamsString());
			}
		}
		else
		{
			System.out.println("Agent : MsgInQueue : Unknown Message");
			System.out.println("MsgType=" + me.getMsgType().toString());
			System.out.println("Region=" + me.getMsgRegion() + " Agent=" + me.getMsgAgent() + " plugin=" + me.getMsgPlugin());
			System.out.println("params=" + me.getParamsString());
		}
     }
     catch(Exception ex)
     {
    	 System.out.println("Agent : MsgRoute : Route Failed " + ex.toString());
     }
			
		
	}
}
