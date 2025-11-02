package com.icm.biometric_zone_gate_api.websocket.utils;

import org.springframework.stereotype.Component;
import java.util.concurrent.*;

@Component
public class DeviceSchedulers {
    public final ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);
}