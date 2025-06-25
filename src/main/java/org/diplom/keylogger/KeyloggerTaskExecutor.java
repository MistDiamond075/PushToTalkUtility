package org.diplom.keylogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class KeyloggerTaskExecutor {
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public KeyloggerTaskExecutor() {

    }

    public static ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
