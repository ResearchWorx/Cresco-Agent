package core;

import shared.MsgEvent;

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

         String callId = "callId-" + AgentEngine.region + "_" + AgentEngine.agent; //calculate callID
         if(rm.getParam(callId) != null) { //send message to RPC hash
             AgentEngine.rpcMap.put(rm.getParam(callId), rm);
             return;
         }

         int routePath = getRoutePath();

         switch (routePath) {
             case 1:  System.out.println("ROUTE CASE 1");
                 break;
             default: System.out.println("ROUTE CASE " + routePath);
                 break;
         }
                 //	AgentEngine.commandExec.cmdExec(me);

     }
     catch(Exception ex)
     {
    	 System.out.println("Agent : MsgRoute : Route Failed " + ex.toString());
     }

	}

    private int getRoutePath() {
        int routePath = -1;
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
                if (rm.getParam("scr_region").equals(AgentEngine.region)) {
                    RXr = "1";
                    if (rm.getParam("scr_agent") != null) {
                        if (rm.getParam("scr_agent").equals(AgentEngine.agent)) {
                            RXa = "1";
                            if (rm.getParam("scr_plugin") != null) {
                                RXp = "1";
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
