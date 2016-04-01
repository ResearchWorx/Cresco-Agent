package channels;

import core.AgentEngine;
import shared.MsgEvent;
import shared.PluginInterface;

public class MsgRoute implements Runnable{

	private MsgEvent rm;
	public MsgRoute(MsgEvent rm)
	{
		this.rm = rm;
	}
	public void run()
	{
     try{
		 if(!getTtl()) { //check ttl
			 return;
		 }


         int routePath = getRoutePath();
         String callId = null;
         MsgEvent re = null;
         switch (routePath) {
             case 52:  //System.out.println("AGENT ROUTE TO EXTERNAL VIA ONTROLLER : 52 "  + rm.getParams());
                       sendToController();
                 break;
             case 53:  //System.out.println("AGENT REGIONAL WATCHDOG : 53 "  + rm.getParams());
                        sendToController();
                 break;
             case 56:  //System.out.println("AGENT ROUTE TO COMMANDEXEC : 56 "  + rm.getParams());
                 re = getCommandExec();
                 break;
             case 61:  //System.out.println("AGENT ROUTE TO COMMANDEXEC : 61 "  + rm.getParams());
                 re = getCommandExec();
                 break;
             default: System.out.println("AGENT ROUTE CASE " + routePath + " " + rm.getParams());
                 break;
         }
         if(re != null)
         {
             re.setReturn(); //reverse to-from for return
             AgentEngine.msgInQueue.offer(re);
         }
                 //	AgentEngine.commandExec.cmdExec(me);

     }
     catch(Exception ex)
     {
         ex.printStackTrace();
    	 System.out.println("Agent : MsgRoute : Route Failed " + ex.toString());
     }

	}

    private MsgEvent getCommandExec() {
        try {
            String callId = "callId-" + AgentEngine.region + "_" + AgentEngine.agent; //calculate callID
            if(rm.getParam(callId) != null) { //send message to RPC hash
                AgentEngine.rpcMap.put(rm.getParam(callId), rm);
            }
            else {
                return AgentEngine.commandExec.cmdExec(rm);
            }
        }
        catch(Exception ex) {
            System.out.println("AgentEngine : MsgRoute : getCommandExec Error : " + ex.getMessage());
        }
        return null;
    }

    private void sendToController()
    {
        try {
            PluginInterface pi = AgentEngine.pluginMap.get(AgentEngine.controllerPluginSlot);
            pi.msgIn(rm); //send msg to plugin
        }
        catch(Exception ex) {
            System.out.println("AgentEngine : MsgRoute Error : " + ex.getMessage());
        }

    }
    private int getRoutePath() {
        int routePath;
        try {
            //determine if local or controller
            String RXr = "0";
            String RXa = "0";
            String RXp = "0";
            String TXr = "0";
            String TXa = "0";
            String TXp = "0";

            if (rm.getParam("dst_region") != null) {
                if (rm.getParam("dst_region").equals(AgentEngine.region)) {
                    RXr = "1";
                    if (rm.getParam("dst_agent") != null) {
                        if (rm.getParam("dst_agent").equals(AgentEngine.agent)) {
                            RXa = "1";
                            if (rm.getParam("dst_plugin") != null) {
                                RXp = "1";
                            }
                        }
                    }
                }

            }

            if (rm.getParam("src_region") != null) {
                if (rm.getParam("src_region").equals(AgentEngine.region)) {
                    TXr = "1";
                    if (rm.getParam("src_agent") != null) {
                        if (rm.getParam("src_agent").equals(AgentEngine.agent)) {
                            TXa = "1";
                            if (rm.getParam("src_plugin") != null) {
                                TXp = "1";
                            }
                        }
                    }
                }

            }

            String routeString = RXr + TXr + RXa + TXa + RXp + TXp;
            routePath = Integer.parseInt(routeString, 2);
        }
        catch(Exception ex)
        {
            System.out.println("AgentEngine : MsgRoute : getRoutePath Error: " + ex.getMessage());
            ex.printStackTrace();
            routePath = -1;
        }
        return routePath;
    }
	private boolean getTtl() {
		boolean isValid = true;
		try {
			if(rm.getParam("ttl") != null) //loop detection
			{
				int ttlCount = Integer.valueOf(rm.getParam("ttl"));

				if(ttlCount > 10)
				{
					System.out.println("**Agent : MsgRoute : High Loop Count**");
					System.out.println("MsgType=" + rm.getMsgType().toString());
					System.out.println("Region=" + rm.getMsgRegion() + " Agent=" + rm.getMsgAgent() + " plugin=" + rm.getMsgPlugin());
					System.out.println("params=" + rm.getParamsString());
					isValid = false;
				}

				ttlCount++;
				rm.setParam("ttl", String.valueOf(ttlCount));
			}
			else
			{
				rm.setParam("ttl", "0");
			}

		}
		catch(Exception ex) {
			isValid = false;
		}
		return isValid;
	}
}
