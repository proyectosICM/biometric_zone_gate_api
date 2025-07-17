package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.dto.GlogEventDTO;
import com.icm.biometric_zone_gate_api.services.DeviceEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/device-event")
public class DeviceEventController {
    private final DeviceEventService deviceEventService;

    // TODO: In case the device ID and token are sent in the header when making a biometric registration
    @PostMapping("/realtime-glog")
    public ResponseEntity<String> recibirGlog(
            @RequestHeader("dev_id") String devId,
            @RequestHeader("dev_model") String devModel,
            @RequestHeader("token") String token,
            @RequestBody GlogEventDTO dto) {

        deviceEventService.processGlogEvent(devId, devModel, token, dto);
        return ResponseEntity.ok("OK");
    }

    //TODO:  In case the device ID is sent in the URL path
    @PostMapping("/realtime-glog/{devId}")
    public ResponseEntity<String> recibirGlogConPath(
            @PathVariable String devId,
            @RequestHeader("dev_model") String devModel,
            @RequestHeader("token") String token,
            @RequestBody GlogEventDTO dto) {

        deviceEventService.processGlogEvent(devId, devModel, token, dto);
        return ResponseEntity.ok("OK");
    }

    //TODO:  In case the device ID is sent in the URL path without token
    @PostMapping("/realtime-glog/{devId}/no-token")
    public ResponseEntity<String> recibirGlogSinToken(
            @PathVariable String devId,
            @RequestHeader("dev_model") String devModel,
            @RequestBody GlogEventDTO dto) {

        //
        deviceEventService.processGlogEvent(devId, devModel, null, dto);
        return ResponseEntity.ok("OK");
    }
}
