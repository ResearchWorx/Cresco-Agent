package channels;

import com.researchworx.cresco.library.messaging.MsgEvent;
import core.AgentEngine;

public class RPCCall {

    public RPCCall() {

    }

    public MsgEvent call(MsgEvent me) {
        try {

            String callId = java.util.UUID.randomUUID().toString();
            me.setParam("callId-" + AgentEngine.region + "_" + AgentEngine.agent, callId);
            me.setParam("is_rpc", "true");
            AgentEngine.msgInQueue.offer(me);
            int count = 0;
            int timeout = 300;
            boolean isWaiting = true;
            while (count < timeout) {
                if (AgentEngine.rpcMap.containsKey(callId)) {
                    MsgEvent ce = null;

                    synchronized (AgentEngine.rpcMap) {
                        ce = AgentEngine.rpcMap.get(callId);
                        AgentEngine.rpcMap.remove(callId);
                    }

                    return ce;
                }
                Thread.sleep(100);
                count++;
            }

            return null;
        } catch (Exception ex) {
            System.out.println("Controller : RPCCall : RPC failed " + ex.toString());
            return null;
        }

    }

}