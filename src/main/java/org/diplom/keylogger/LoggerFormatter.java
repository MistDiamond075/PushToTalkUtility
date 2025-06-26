package org.diplom.keylogger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggerFormatter extends Formatter {

    public enum Colors{
        RESET( "\u001B[0m"),
        BLACK("\u001B[90m"),
        RED("\u001B[91m"),
        GREEN("\u001B[92m"),
        YELLOW("\u001B[93m"),
        BLUE("\u001B[94m"),
        PURPLE("\u001B[95m"),
        CYAN("\u001B[96m"),
        WHITE("\u001B[97m");

        public final String color;

        private static final Colors[] colorValues = {
                BLACK, RED, GREEN, YELLOW, BLUE, PURPLE, CYAN, WHITE
        };

        private static final Random random = new Random();

        public static Colors randomColor() {
            return colorValues[random.nextInt(colorValues.length)];
        }

        Colors(String color){
            this.color = color;
        }
    }

    @Override
    public String format(LogRecord record) {
        String color;
        switch (record.getLevel().getName()) {
            case "SEVERE" -> color = Colors.RED.color;
            case "WARNING" -> color = Colors.YELLOW.color;
            case "INFO" -> color = Colors.GREEN.color;
            case "FINE" -> color = Colors.PURPLE.color;
            default -> color = Colors.RESET.color;
        }
        Instant instant = Instant.ofEpochMilli(record.getMillis());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm:ss VV");
        return Colors.CYAN.color+
                "["+ ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter) +"] "+
                color+
                "["+record.getLevel().getName()+"] "+
                Colors.WHITE.color+
                record.getMessage()+
                Colors.RESET.color+
                '\n';
    }

    public LoggerFormatter() {

    }
}
