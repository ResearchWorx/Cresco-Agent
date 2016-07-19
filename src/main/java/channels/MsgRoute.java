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
            if (rm.getMsgType() != MsgEvent.Type.WATCHDOG && rm.getMsgType() != MsgEvent.Type.LOG && rm.getMsgType() != MsgEvent.Type.KPI && rm.getMsgType() != MsgEvent.Type.INFO) {
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
            logger.error("Agent : MsgRoute : Route Failed " + ex.toString());
        }

    }

    private MsgEvent getCommandExec() {
        logger.trace("Call to getCommandExec()");
        logger.debug("getCommandExec() : rm.getParams() = {}", rm.getParams());
        try {
            logger.trace("Calculating callId");
            String callId = "callId-" + AgentEngine.region + "_" + AgentEngine.agent; //calculate callID
            if (rm.getParam(callId) != null) { //send message to RPC hash
                logger.debug("callId={}", callId);
                logger.trace("Putting on RPC map");
                AgentEngine.rpcMap.put(rm.getParam(callId), rm);
            } else {
                logger.trace("Executing message");
                return AgentEngine.commandExec.cmdExec(rm);
            }
        } catch (Exception ex) {
            logger.error("getCommandExec : " + ex.getMessage());
        }
        return null;
    }

    private void sendToController() {
        try {
            AgentEngine.pluginMap.get(AgentEngine.controllerPluginSlot).Message(rm);
        } catch (Exception e) {
            logger.error("sendToController : " + e.getMessage());
        }

    }

    private void sendToPlugin() {
        logger.trace("Call to sendToPlugin()");
        logger.debug("sendToPlugin() : rm.getParams() = {}", rm.getParams());
        try {
            if (rm.getParam("dst_plugin") != null) {
                if (AgentEngine.pluginMap.containsKey(rm.getParam("dst_plugin"))) {
                    logger.trace("Sending to plugin");
                    AgentEngine.pluginMap.get(rm.getParam("dst_plugin")).Message(rm);
                } else {
                    logger.trace("sendToPlugin : Plugin=" + rm.getParam("dst_plugin") + " not found, dropping");
                }
            } else {
                logger.debug("sendToPlugin : no 'dst_plugin' found in MsgEvent, dropping");
            }
        } catch (Exception e) {
            logger.error("sendToPlugin : " + e.getMessage());
            logger.debug("Message params: {}", rm.getParams());
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
            logger.error("getRoutePath : " + ex.getMessage());
            routePath = -1;
        }
        return routePath;
    }

    private boolean getTTL() {
        boolean isValid = true;
        try {
            if (rm.getParam("ttl") != null) //loop detection
            {
                int ttlCount = Integer.valueOf(rm.getParam("ttl"));

                if (ttlCount > 10) {
                    logger.error("**Agent : MsgRoute : High Loop Count**");
                    logger.error("MsgType=" + rm.getMsgType().toString());
                    logger.error("Region=" + rm.getMsgRegion() + " Agent=" + rm.getMsgAgent() + " plugin=" + rm.getMsgPlugin());
                    logger.error("params=" + rm.getParams());
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
