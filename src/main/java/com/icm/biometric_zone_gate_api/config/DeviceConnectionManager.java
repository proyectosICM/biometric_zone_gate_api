package com.icm.biometric_zone_gate_api.config;

import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceWebSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DeviceConnectionManager {

    private final DeviceRepository deviceRepository;

    // This provider creates DeviceWebSocketClient instances with injected dependencies.
    private final ObjectProvider<DeviceWebSocketClient> clientProvider;

    private final Map<Long, DeviceWebSocketClient> activeConnections = new ConcurrentHashMap<>();

    public void connectAllDevices() {
        List<DeviceModel> devices = deviceRepository.findAll();

        if (devices.isEmpty()) {
            System.out.println("There are no devices registered in the database.");
            return;
        }

        for (DeviceModel device : devices) {
            connectDevice(device);
        }
    }

    public void connectDevice(DeviceModel device) {
        String host = device.getHost();
        if (host == null || host.isEmpty()) {
            System.out.println("The device " + device.getSn() + " does not have an IP address configured, so it is omitted.");
            return;
        }

        try {
            System.out.println("Connecting the device " + device.getName() + " (" + host + ")...");

            // Spring creates a new correctly injected instance
            DeviceWebSocketClient client = clientProvider.getObject();
            client.connect(host);

            activeConnections.put(device.getId(), client);
            System.out.println("Correctly connected to " + device.getName());

        } catch (Exception e) {
            System.err.println("Error connecting to " + host + ": " + e.getMessage());
        }
    }

    public void disconnectDevice(Long deviceId) {
        DeviceWebSocketClient client = activeConnections.remove(deviceId);
        if (client != null) {
            client.disconnect();
            System.out.println("Device " + deviceId + " offline.");
        }
    }

    public void disconnectAll() {
        activeConnections.values().forEach(DeviceWebSocketClient::disconnect);
        activeConnections.clear();
        System.out.println("All devices have been disconnected.");
    }

    public DeviceWebSocketClient getClient(Long deviceId) {
        return activeConnections.get(deviceId);
    }
}
