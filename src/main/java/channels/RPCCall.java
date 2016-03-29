package channels;

import shared.MsgEvent;
import core.AgentEngine;

public class RPCCall {

    public RPCCall()
    {

    }
    public MsgEvent call(MsgEvent me)
    {
        try
        {

            String callId = java.util.UUID.randomUUID().toString();
            me.setParam("callId-" + AgentEngine.region + "_" + AgentEngine.agent, callId);
            AgentEngine.msgInQueue.offer(me);
            System.out.println("SENT RPC MESSAGE= " + me.getParams());
            int count = 0;
            int timeout = 300;
            while(count < timeout)
            {
                if(AgentEngine.rpcMap.containsKey(callId))
                {
                    MsgEvent ce = null;

                    synchronized (AgentEngine.rpcMap)
                    {
                        ce =  AgentEngine.rpcMap.get(callId);
                        AgentEngine.rpcMap.remove(callId);
                    }

                    return ce;
                }
                Thread.sleep(100);
                count++;
            }

            return null;
        }
        catch(Exception ex)
        {
            System.out.println("Controller : RPCCall : RPC failed " + ex.toString());
            return null;
        }

    }

}