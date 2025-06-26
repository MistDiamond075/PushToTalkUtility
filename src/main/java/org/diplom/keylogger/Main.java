package org.diplom.keylogger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.fusesource.jansi.AnsiConsole;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.logging.*;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private enum Mode{DEBUG,NORMAL}
    private static final String OSname=System.getProperty("os.name").toLowerCase();;
    private static Mode mode;
    private static final String defaultPort="60602";
    private static boolean pressed=false;
    private static boolean released=false;
    private static final Set<Integer> keys= new HashSet<>(2);
    private static boolean criticalError=false;

    public static void main( String[] arguments) {
        System.setProperty("jansi.force", "true");
        AnsiConsole.systemInstall();
        Map<String, String> args = Parser.parseArgs(arguments);
        String strPort = args.getOrDefault("port", defaultPort);
        try {
            mode = Mode.valueOf(args.getOrDefault("mode", "NORMAL").toUpperCase());
        }catch (IllegalArgumentException e){
            logger.warning("incorrect mode "+mode+". Set normal mode");
            mode = Mode.NORMAL;
        }finally {
            setLoggerSettings(logger);
            logger.info("info level: "+mode.toString());
        }
        setLoggerSettings(Parser.getLogger());
        int port;
        try {
             port= Integer.parseInt(strPort);
             logger.info("using port "+port);
        }catch (NumberFormatException | NullPointerException e) {
            logger.warning("an error occurred when trying to set "+strPort+" port. Using default port "+defaultPort+" instead");
            port=60602;
        }
        boolean autostartRequired= !args.containsKey("disable_autostart");
        if(autostartRequired) {
            boolean autostartInstalled=Autostart.isAutostartRegistered();
            if(!autostartInstalled) {
                setLoggerSettings(Autostart.getLogger());
                logger.info("setting autostart...");
                Autostart.setAutostart();
            }else {
                logger.info("autostart is already set");
            }
        }else{
            logger.info("autostart disabled");
        }
        KeyloggerWebSocket ws=new KeyloggerWebSocket(port);
        ws.start();
        setShutdownEventListener(ws);
        setLoggerSettings(ws.getLogger());
        ws.waitForConnection();
        try {
            GlobalScreen.registerNativeHook();
            if(mode==Mode.DEBUG) {
                System.out.println();
            }
            logger.fine("registered Native Hook");
        } catch (Exception e) {
            logger.severe("There was a problem registering the native hook.");
        }

        JSONObject json=new JSONObject();

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                for (Integer key : keys) {
                    if (e.getKeyCode() == key) {
                        logger.fine("pressed "+e.getKeyCode());
                        json.put("event", "pressed");
                        json.put("key", key);
                        if(!pressed) {
                            ws.sendResponse(json.toString());
                            released=false;
                        }
                        pressed=true;
                    }
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                for (Integer key : keys) {
                    if (e.getKeyCode() == key) {
                        logger.fine("released "+e.getKeyCode());
                        json.put("event", "released");
                        json.put("key", key);
                        if(!released) {
                            ws.sendResponse(json.toString());
                            pressed=false;
                        }
                        released=true;
                    }
                }
            }
        });
    }

    private static void setLoggerSettings(Logger logger) {
        LoggerFormatter formatter=new LoggerFormatter();
        logger.setLevel(mode==Mode.NORMAL ? Level.INFO : Level.ALL);
       logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        handler.setLevel(mode==Mode.NORMAL ? Level.INFO : Level.ALL);
        logger.addHandler(handler);
    }

    public static void setKeys(List<String> keysArray) {
        for (String key : keysArray) {
            logger.fine("parsing key "+key);
            keys.add(Parser.parseKey(key));
        }
    }

    public static void removeOldKeys(){
        keys.clear();
        logger.fine("old keys removed");
    }

    public static void setCriticalError(boolean criticalError) {
        Main.criticalError = criticalError;
    }

    private static void setShutdownEventListener(KeyloggerWebSocket ws){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            try {
                KeyloggerTaskExecutor.getScheduler().shutdownNow();
                GlobalScreen.unregisterNativeHook();
                JSONObject msg = new JSONObject()
                .put("event", "shutdown");
                if (ws != null) {
                    ws.sendResponse(msg.toString());
                    Thread.sleep(500);
                    ws.stop();
                }
            } catch (Exception e) {
                logger.severe("unable to send websocket shutdown message: "+e.getMessage());
                logger.severe(Arrays.toString(e.getStackTrace()));
            }
            if(criticalError) {
                System.out.println(LoggerFormatter.Colors.RESET.color+"window will be automatically closed in 15 seconds...");
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    System.exit(1);
                }
            }
        }));
    }
}
