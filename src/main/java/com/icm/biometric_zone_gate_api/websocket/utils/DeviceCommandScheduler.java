package com.icm.biometric_zone_gate_api.websocket.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DeviceCommandScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public void schedule(Runnable task, long delayMs) {
        scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }
}
