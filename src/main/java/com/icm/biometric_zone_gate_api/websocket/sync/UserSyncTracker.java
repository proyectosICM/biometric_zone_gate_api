package com.icm.biometric_zone_gate_api.websocket.sync;

import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class UserSyncTracker {
    public final String deviceSn;
    public final AtomicInteger pending;
    public final int expected;
    public volatile ScheduledFuture<?> timeoutFuture;
    public volatile ZonedDateTime startedAt;

    public UserSyncTracker(String deviceSn, int expected) {
        this.deviceSn = deviceSn;
        this.expected = expected;
        this.pending = new AtomicInteger(expected);
    }
}

