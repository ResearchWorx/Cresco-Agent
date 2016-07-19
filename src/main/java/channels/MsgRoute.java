package channels;

import com.researchworx.cresco.library.messaging.MsgEvent;
import core.AgentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgRoute implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("Routing");

    private MsgEvent rm;

    public MsgRoute(MsgEvent rm) {
        this.rm = rm;
    }

    public void run() {
        try {
            if (!getTTL()) {
                return;
            }

            int routePath = getRoutePath();
            if (rm.getMsgType() != MsgEvent.Type.WATCHDOG || rm.getMsgType() != MsgEvent.Type.LOG) {
                logger.debug("Routing: Path={}, Type={}, Src={}-{}:{}, Dst={}-{}:{}, Params={}", routePath,
                        rm.getMsgType().name(),
                        rm.getParam("src_region"), rm.getParam("src_agent"), rm.getParam("src_plugin"),
                        rm.getParam("dst_region"), rm.getParam("dst_agent"), rm.getParam("dst_plugin"),
                        rm.getParams());
            }
            MsgEvent re = null;
            switch (routePath) {
                case 52:  //System.out.println("AGENT ROUTE TO EXTERNAL VIA CONTROLLER : 52 "  + rm.getParams());
                    if (rm.getMsgType() == MsgEvent.Type.LOG)
                        re = getCommandExec();
                    else
                        sendToController();
                    break;
                case 53:  //System.out.println("AGENT REGIONAL WATCHDOG : 53 "  + rm.getParams());
                    if (rm.getMsgType() == MsgEvent.Type.LOG)
                        re = getCommandExec();
                    else
                        sendToController();
                    break;
                case 56:  //System.out.println("AGENT ROUTE TO COMMANDEXEC : 56 "  + rm.getParams());
                    re = getCommandExec();
                    break;
                case 58:  //System.out.println("AGENT ROUTE CASE 58  " + rm.getParams());
                    sendToPlugin(); //remote plugin to local plugin
                    break;
                case 61:  //System.out.println("AGENT ROUTE TO COMMANDEXEC : 61 "  + rm.getParams());
                    re = getCommandExec();
                    break;
                case 62:  //System.out.println("PLUGIN RPC CALL TO HOST AGENT : 62 " + rm.getParams());
                    sendToPlugin();
                    break;
                case 63: //System.out.println("PLUGIN DIRECT MESSAGE TO ANOTHER PLUGIN ON SAME AGENT : 63 " + rm.getParams());
                    sendToPlugin();
                    break;
                default:
                    if (rm.getMsgType() == MsgEvent.Type.LOG)
                        re = getCommandExec();
                    else
                        logger.debug("AGENT ROUTE CASE " + routePath + " " + rm.getParams());
                    break;
            }
            if (re != null) {
                re.setReturn(); //reverse to-from for return
                logger.debug("Forwarding: Type={}, Src={}-{}:{}, Dst={}-{}:{}, Params={}", re.getMsgType().name(),
                        re.getParam("src_region"), re.getParam("src_agent"), re.getParam("src_plugin"),
                        re.getParam("dst_region"), re.getParam("dst_agent"), re.getParam("dst_plugin"),
                        re.getParams());
                AgentEngine.msgInQueue.offer(re);
                //new Thread(new MsgRoute(re)).start();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Agent : MsgRoute : Route Failed " + ex.toString());
        }

    }

    private MsgEvent getCommandExec() {
        try {
            String callId = "callId-" + AgentEngine.region + "_" + AgentEngine.agent; //calculate callID
            if (rm.getParam(callId) != null) { //send message to RPC hash
                AgentEngine.rpcMap.put(rm.getParam(callId), rm);
            } else {
                return AgentEngine.commandExec.cmdExec(rm);
            }
        } catch (Exception ex) {
            System.out.println("AgentEngine : MsgRoute : getCommandExec Error : " + ex.getMessage());
        }
        return null;
    }

    private void sendToController() {
        try {
            AgentEngine.pluginMap.get(AgentEngine.controllerPluginSlot).Message(rm);
        } catch (Exception e) {
            System.out.println("AgentEngine : sendToController Error : " + e.getMessage());
        }

    }

    private void sendToPlugin() {
        try {
            if(rm.getParam("dst_plugin") != null) {
                if(AgentEngine.pluginMap.containsKey(rm.getParam("dst_plugin"))) {
                    AgentEngine.pluginMap.get(rm.getParam("dst_plugin")).Message(rm);
                } else {
                    System.out.println("ERROR : sendToPlugin : Plugin=" + rm.getParam("dst_plugin") + " not found");
                }
            } else {
                System.out.println("ERROR : sendToPlugin : no dst_plugin found in MsgEvent");
            }

        } catch (Exception e) {
            System.out.println("AgentEngine : sendToPlugin : " + e.getMessage());
            e.printStackTrace();
            System.out.println(rm.getParams());
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
        } catch (Exception ex) {
            System.out.println("AgentEngine : MsgRoute : getRoutePath Error: " + ex.getMessage());
            ex.printStackTrace();
            routePath = -1;
        }
        //System.out.println("AGENT ROUTEPATH=" + routePath + " MsgType=" + rm.getMsgType() + " Params=" + rm.getParams());

        return routePath;
    }

    private boolean getTTL() {
        boolean isValid = true;
        try {
            if (rm.getParam("ttl") != null) //loop detection
            {
                int ttlCount = Integer.valueOf(rm.getParam("ttl"));

                if (ttlCount > 10) {
                    System.out.println("**Agent : MsgRoute : High Loop Count**");
                    System.out.println("MsgType=" + rm.getMsgType().toString());
                    System.out.println("Region=" + rm.getMsgRegion() + " Agent=" + rm.getMsgAgent() + " plugin=" + rm.getMsgPlugin());
                    System.out.println("params=" + rm.getParams());
                    isValid = false;
                }

                ttlCount++;
                rm.setParam("ttl", String.valueOf(ttlCount));
            } else {
                rm.setParam("ttl", "0");
            }

        } catch (Exception ex) {
            isValid = false;
        }
        return isValid;
    }
}
