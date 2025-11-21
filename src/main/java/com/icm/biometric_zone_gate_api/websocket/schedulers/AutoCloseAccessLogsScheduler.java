package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.services.AccessLogsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoCloseAccessLogsScheduler {

    private final AccessLogsService accessLogsService;

    /**
     * Corre cada 1 minuto (ajústalo si quieres).
     * fixedDelay = 60000 → 60,000 ms = 1 minuto
     */
    @Scheduled(fixedDelay = 60000)
    public void autoCloseOldLogs() {
        int minutes = 30;
        accessLogsService.autoCloseOldOpenLogs(minutes, "cerrado por el sistema");
    }
}
