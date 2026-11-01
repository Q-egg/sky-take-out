package com.sky.websocket;


import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/{id}")
public class WebSocketServer {


    //存放会话对象
    private static Map<String, Session> sessionMap = new HashMap<>();


    /**
     * 连接建立成功调用方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id) {
        sessionMap.put(id, session);
    }

    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, @PathParam("id") String id) {
    }

    /**
     * 连接关闭调用方法
     */
    @OnClose
    public void onClose(@PathParam("id") String id) {
        sessionMap.remove(id);
    }


    /**
     * 群发
     */
    public void sendToAllClient(String message){
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try{
                //服务端向客户端发送消息
                session.getAsyncRemote().sendText(message);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
