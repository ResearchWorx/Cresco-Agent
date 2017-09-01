package channels;

import com.researchworx.cresco.library.messaging.MsgEvent;
import core.AgentEngine;

import java.util.Timer;
import java.util.TimerTask;

public class RPCCall_old {

    private Timer timer;
    private long timeout = 15000L;
    private boolean timerActive = false;

    public RPCCall_old() {
        timer = new Timer();

    }

    private class StopListenerTask extends TimerTask {
        public void run() {
            timerActive = false;
        }
    }

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