package org.diplom.keylogger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class KeyloggerWebSocket extends WebSocketServer {
    private CompletableFuture<WebSocket> client=new CompletableFuture<>();
    private static final Logger logger = Logger.getLogger(KeyloggerWebSocket.class.getName());
    private int pingMisses=0;

    public KeyloggerWebSocket(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!client.isDone()) {
            client.complete(conn);
            System.out.println();
            logger.info("connected to client");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        this.client = new CompletableFuture<>();
        logger.info("disconnected from client. Reason: " + reason+'\t'+"Code: "+code);
        Main.removeOldKeys();
        waitForConnection();
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        JSONObject msg=new JSONObject(s);
        String event=msg.getString("event");
        logger.fine("received message: "+ s);
        if(event.equals("pong")){
            pingMisses=0;
        }else if(event.equals("connected")){
            JSONArray keysArray=msg.getJSONArray("keys");
            ArrayList<String> keys= new ArrayList<>();
            for(int i=0;i<keysArray.length();i++){
                keys.add(keysArray.getString(i));
            }
            Main.setKeys(keys);
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        if(e instanceof IOException || e instanceof SecurityException){
            logger.severe("websocket critical error: " + e.getMessage());
            Main.setCriticalError(true);
            System.exit(1);
        }else {
            logger.warning("websocket error: " + e.getMessage());
        }
    }

    @Override
    public void onStart() {
        logger.fine("websocket server started");
        if(client.isDone()) {
            KeyloggerTaskExecutor.getScheduler().scheduleAtFixedRate(() -> {
                JSONObject request = new JSONObject();
                request.put("event", "ping");
                sendResponse(request.toString());
                pingMisses++;
                logger.fine("sending ping. Ping misses: " + pingMisses);
                if (pingMisses > 3) {
                    try {
                        client.get().close();
                        pingMisses = 0;
                    } catch (InterruptedException | ExecutionException e) {
                        logger.severe("an error occurred when trying to ping client. Reason: " + e.getMessage());
                    }
                }
            }, 0, 30, TimeUnit.SECONDS);
        }
    }

    public void sendResponse(String response) {
        if(client.isDone()) {
            try {
                WebSocket conn = client.get();
                if (conn != null && conn.isOpen()) {
                    logger.fine("sending response: " + response);
                    conn.send(response);
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.severe("an error occurred with websocket connection. Reason: " + e.getMessage());
            }
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void waitForConnection(){
        AtomicInteger dotCount = new AtomicInteger(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm:ss VV");
        String now = ZonedDateTime.now().format(formatter);
        ScheduledFuture<?>[] task = new ScheduledFuture<?>[1];
        task[0] = KeyloggerTaskExecutor.getScheduler().scheduleAtFixedRate(() -> {
            if (client.isDone()) {
                System.out.println();
                task[0].cancel(true);
                return;
            }
            int count = dotCount.updateAndGet(n -> (n % 6) +1);
           // logger.fine("points count: "+count);
            System.out.print(
                    LoggerFormatter.Colors.CYAN.color+
                    "\r["+ now +"] "+
                    LoggerFormatter.Colors.GREEN.color +"[INFO] "+
                    LoggerFormatter.Colors.WHITE.color+ "waiting for client connection"+
                    LoggerFormatter.Colors.randomColor().color + ".".repeat(count-1)
            );
            System.out.flush();
        }, 0, 700, TimeUnit.MILLISECONDS);
    }
}
