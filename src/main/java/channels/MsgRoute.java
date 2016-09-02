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
            if (rm.getMsgType() == MsgEvent.Type.LOG) {
                getCommandExec();
                return;
            }
            if (!getTTL()) {
                return;
            }


            int routePath = getRoutePath();
            //logger.info("routepath: " + routePath + "[" + rm.getParams().toString() + "]");
            if (rm.getMsgType() != MsgEvent.Type.WATCHDOG && rm.getMsgType() != MsgEvent.Type.LOG && rm.getMsgType() != MsgEvent.Type.KPI && rm.getMsgType() != MsgEvent.Type.INFO) {
                logger.debug("Routing: Path={}, Type={}, Src={}-{}:{}, Dst={}-{}:{}, Params={}", routePath,
                        rm.getMsgType().name(),
                        rm.getParam("src_region"), rm.getParam("src_agent"), rm.getParam("src_plugin"),
                        rm.getParam("dst_region"), rm.getParam("dst_agent"), rm.getParam("dst_plugin"),
                        rm.getParams());
            }
            if (routePath < 56) {
                sendToController();
            } else {
                MsgEvent re = null;
                switch (routePath) {
                    /*case 50:
                        logger.trace("Case 50: Intra-region, from another agent, to another agent's plugin");
                        sendToController();
                        break;
                    case 51:
                        logger.trace("Case 51: Intra-region, from another agent's plugin, to another agent's plugin");
                        sendToController();
                        break;
                    case 52:
                        logger.trace("Case 52: Intra-region, from this agent, to another agent");
                        sendToController();
                        break;
                    case 53:
                        logger.trace("Case 53: Intra-region, from this agent, to another agent's plugin");
                        sendToController();
                        break;
                    case 54:
                        logger.trace("Case 54: Intra-region, from this agent, to another agent's plugin");
                        sendToController();
                        break;
                    case 55:
                        logger.trace("Case 55: Intra-region, from a plugin on this agent, to a plugin on another agent");
                        sendToController();
                        break;*/
                    case 56:
                        logger.trace("Case 56: Intra-region, from another agent, to this agent");
                        re = getCommandExec();
                        break;
                    case 57:
                        logger.trace("Case 57: Intra-region, from another agent's plugin, to this agent");
                        re = getCommandExec();
                        break;
                    case 58:
                        logger.trace("Case 58: Intra-region, from another agent, to a plugin on this agent");
                        sendToPlugin();
                        break;
                    case 59:
                        logger.trace("Case 59: Intra-region, from another agent's plugin, to a plugin on this agent");
                        sendToPlugin();
                        break;
                    case 60:
                        logger.trace("Case 60: Intra-region, intra-agent");
                        re = getCommandExec();
                        break;
                    case 61:
                        logger.trace("Case 61: Intra-region, intra-agent, plugin to agent");
                        re = getCommandExec();
                        break;
                    case 62:
                        logger.trace("Case 62: Intra-region, intra-agent, agent to plugin");
                        sendToPlugin();
                        break;
                    case 63:
                        logger.trace("Case 63: Intra-region, intra-agent, plugin to plugin");
                        sendToPlugin();
                        break;
                    default:
                        logger.error("Case {}: Unknown route. rm.getParams() = {}", routePath, rm.getParams());
                        break;
                }
                if (re != null) {
                    re.setReturn();
                    logger.debug("Forwarding: Type={}, Src={}-{}:{}, Dst={}-{}:{}, Params={}", re.getMsgType().name(),
                            re.getParam("src_region"), re.getParam("src_agent"), re.getParam("src_plugin"),
                            re.getParam("dst_region"), re.getParam("dst_agent"), re.getParam("dst_plugin"),
                            re.getParams());
                    AgentEngine.msgInQueue.offer(re);
                }
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
