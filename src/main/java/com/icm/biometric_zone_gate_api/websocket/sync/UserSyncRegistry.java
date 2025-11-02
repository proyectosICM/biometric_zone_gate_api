package com.icm.biometric_zone_gate_api.websocket.sync;

// package ...websocket.sync
import org.springframework.stereotype.Component;
import java.util.concurrent.*;

@Component
public class UserSyncRegistry {

    private final ConcurrentHashMap<String, UserSyncTracker> map = new ConcurrentHashMap<>();

    public void start(String sn, int expected) {
        map.put(sn, new UserSyncTracker(sn, expected));
    }

    public UserSyncTracker get(String sn) {
        return map.get(sn);
    }

    public void clear(String sn) {
        map.remove(sn);
    }

    public boolean decrementAndIsDone(String sn) {
        UserSyncTracker t = map.get(sn);
        if (t == null) return true; // nada que esperar
        int left = t.pending.decrementAndGet();
        return left <= 0;
    }
}

