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
                          MsgEvent me = AgentEngine.msgInQueue.take(); //get logevent
                          AgentEngine.msgIn(me);
                } else {
                    Thread.sleep(100);
                }

            } catch (Exception ex) {
                System.out.println("Agent : MsgInQueue Error :" + ex.getMessage());
            }

        }
        //shutdown was called
    }

}