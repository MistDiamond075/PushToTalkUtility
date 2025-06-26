package org.diplom.keylogger;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Parser {
    private static final Logger logger = Logger.getLogger(Parser.class.getName());

    public static int parseKey(String strKey){
        try {
            String constName = "VC_" + strKey.toUpperCase();
            Field field = NativeKeyEvent.class.getField(constName);
            logger.fine("parsed key: "+field.getInt(null));
            return field.getInt(null);
        } catch (Exception e) {
            logger.warning("undefined key " + strKey);
            return NativeKeyEvent.VC_UNDEFINED;
        }
    }

    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        logger.fine("args: "+ Arrays.toString(args));
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] split = arg.substring(2).split("=", 2);
                if (split.length == 2) {
                    map.put(split[0], split[1]);
                } else {
                    map.put(split[0], "true");
                }
            } else if (arg.startsWith("pttutility://")) {
                try {
                    URI uri = new URI(arg);
                    String query = uri.getQuery();
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length == 2) {
                                map.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                            }
                        }
                    }
                } catch (URISyntaxException e) {
                    logger.warning("URI parse error: " + arg);
                }
            }
        }
        return map;
    }

    public static Logger getLogger() {
        return logger;
    }
}
