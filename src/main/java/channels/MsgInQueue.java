package channels;

import com.researchworx.cresco.library.messaging.MsgEvent;
import core.AgentEngine;
import core.CommandExec;


public class MsgInQueue implements Runnable {

    private CommandExec commandExec;

    public MsgInQueue() {
        commandExec = new CommandExec();
    }

    public void run() {
        AgentEngine.MsgInQueueEnabled = true;
        while (AgentEngine.MsgInQueueEnabled) {
            try {
                if (AgentEngine.MsgInQueueActive) {
                    synchronized (AgentEngine.msgInQueue) {
                        while ((!AgentEngine.msgInQueue.isEmpty()) && AgentEngine.MsgInQueueEnabled) {
                            MsgEvent me = AgentEngine.msgInQueue.poll(); //get logevent
                            //new MsgRoute(me).run(); //route messages in new thread
                            AgentEngine.msgIn(me);
                        }
                    }
                }
                Thread.sleep(100);
            } catch (Exception ex) {
                System.out.println("Agent : MsgInQueue Error :" + ex.getMessage());
            }

        }
        //shutdown was called
    }
}