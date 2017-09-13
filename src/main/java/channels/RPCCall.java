package channels;

import com.researchworx.cresco.library.messaging.MsgEvent;
import core.AgentEngine;

import java.util.Timer;
import java.util.TimerTask;

public class RPCCall {

    private Timer timer;
    private long timeout = 15000L;
    private boolean timerActive = false;

    public RPCCall() {
        timer = new Timer();

    }

    private class StopListenerTask extends TimerTask {
        public void run() {
            timerActive = false;
        }
    }

    public MsgEvent call2(MsgEvent msg) {
        try {
            int MAX_INTERVALS = 10;

            String callId = java.util.UUID.randomUUID().toString();
            msg.setParam("callId-" + AgentEngine.region + "_" + AgentEngine.agent, callId);
            msg.setParam("is_rpc", "true");
            AgentEngine.msgInQueue.add(msg);

            Object receipt = new Object();
            AgentEngine.rpcReceipts.put(callId, receipt);

            int count = 0;
            while (count++ < MAX_INTERVALS) {
                synchronized(receipt) {
                    receipt.wait();
                }
                    if (AgentEngine.rpcMap.containsKey(callId)) {
                        MsgEvent callBack;
                        callBack = AgentEngine.rpcMap.get(callId);
                        AgentEngine.rpcMap.remove(callId);
                        return callBack;
                    }

            }
        } catch (Exception ex) {
            System.out.println("Controller : RPCCall : RPC failed " + ex.toString());
        }
        return null;
    }

    //todo find blocking method for rpc to prevent high cpu during wait on mesage return
    public MsgEvent call(MsgEvent me) {
        try {

            
            timerActive = true;

            timer.schedule(new StopListenerTask(), timeout);

            String callId = java.util.UUID.randomUUID().toString();
            me.setParam("callId-" + AgentEngine.region + "_" + AgentEngine.agent, callId);
            me.setParam("is_rpc", "true");

            AgentEngine.msgInQueue.add(me);

            MsgEvent ce = null;
            while (timerActive) {

                if (AgentEngine.rpcMap.containsKey(callId)) {
                    timer.cancel();
                    //synchronized (AgentEngine.rpcMap) {
                    ce = AgentEngine.rpcMap.get(callId);
                    AgentEngine.rpcMap.remove(callId);
                    //}
                    return ce;
                } else {
                    //Thread.sleep(1);
                    //synchronized (AgentEngine.rpcMap) {
                        //AgentEngine.rpcMap.wait();
                    //}
                }

            }
            return null;
        } catch (Exception ex) {
            System.out.println("Controller : RPCCall : RPC failed " + ex.toString());
            return null;
        }

    }

}