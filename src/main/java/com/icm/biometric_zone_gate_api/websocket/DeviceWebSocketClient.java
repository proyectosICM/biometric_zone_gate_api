package com.icm.biometric_zone_gate_api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.models.CompanyModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.CompanyRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.services.AccessLogsService;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;

@ClientEndpoint
@Component
@RequiredArgsConstructor
public class DeviceWebSocketClient {

    private final AccessLogsService accessLogsService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Session session;
    private String host;

    // =========================
    // Connection and disconnection
    // =========================
    public void connect(String host) {
        this.host = host;
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(this, new URI("ws://" + host + ":7788"));
            System.out.println("Connected to the device in: " + host);
        } catch (Exception e) {
            System.err.println("Failed to connect to device, retrying in 5s...");
            scheduleReconnect();
        }
    }

    public void disconnect() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
                System.out.println("Connection closed with the device in: " + host);
            } else {
                System.out.println("There is no active session to close in" + host);
            }
        } catch (Exception e) {
            System.err.println("Error closing connection with " + host + ": " + e.getMessage());
        }
    }

    // =========================
    // WebSocket Events
    // =========================
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open connection to the device:");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Message received from " + host + ": " + message);

        try {
            JsonNode json = objectMapper.readTree(message);
            String cmd = json.get("cmd").asText();

            switch (cmd) {
                case "reg" -> handleRegister(json);
                //case "sendlog" -> handleLogs(json);
                case "sendlog" -> handleSendLog(json);
                case "senduser" -> handleNewUser(json);
                case "event" -> handleEvent(json);
                case "senduserlist" -> handleUserList(json);
                case "getnewlog" -> handleLogDownload(json, true);
                case "getalllog" -> handleLogDownload(json, false);
                case "setuserinfo" -> handleDeviceResponse(json, "Set user info");
                case "deleteuser" -> handleDeviceResponse(json, "Delete user");
                case "clearuser" -> handleDeviceResponse(json, "Clear all users");
                case "getuserlist" -> handleDeviceResponse(json, "User list request");
                //case "getalllog" -> handleDeviceResponse(json, "Log list request");
                default -> System.out.println("Command not handled: " + cmd);
            }

        } catch (IOException e) {
            System.err.println("Error processing message from device: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Closed connection: " + reason);
        scheduleReconnect();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error in WebSocket with " + host + ": " + throwable.getMessage());
        scheduleReconnect();
    }

    // =========================
    // Reconnection logic
    // =========================

    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                System.out.println("Retrying connection with " + host + "...");
                connect(host);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    // =========================
    // Command handlers
    // =========================
    private void handleDeviceResponse(JsonNode json, String actionName) {
        String ret = json.has("ret") ? json.get("ret").asText() : "unknown";
        String sn = json.has("sn") ? json.get("sn").asText() : "N/A";

        switch (ret.toLowerCase()) {
            case "ok", "success" -> System.out.println("[" + actionName + "] succeeded for device " + sn);
            case "fail", "error" -> System.err.println("[" + actionName + "] failed for device " + sn);
            default -> System.out.println("ℹ️ [" + actionName + "] response: " + ret + " (device " + sn + ")");
        }
    }

    private void handleRegister(JsonNode json) {
        String sn = json.get("sn").asText();
        String language = json.has("language") ? json.get("language").asText() : "es";

        DeviceModel device = deviceRepository.findBySn(sn).orElse(new DeviceModel());
        device.setSn(sn);
        //device.setLanguage(language);
        device.setHost(host);
        device.setPort("7788");

        // Assign default company (example ID = 1)
        CompanyModel defaultCompany = companyRepository.findById(1L).orElseThrow();
        device.setCompany(defaultCompany);

        deviceRepository.save(device);
        System.out.println("Device registered/updated: " + sn);
    }

    private void handleLogs(JsonNode json) {
        String sn = json.get("sn").asText();
        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        for (JsonNode record : json.get("record")) {
            Long userId = record.get("enrollid").asLong();
            int inout = record.get("inout").asInt();
            String timestamp = record.get("time").asText();

            // Search user or create placeholder
            UserModel user = userRepository.findById(userId).orElseGet(() -> {
                UserModel placeholder = new UserModel();
                placeholder.setId(userId); // usar enrollid como ID
                placeholder.setName("User " + userId);
                placeholder.setEmail("desconocido_" + userId + "@placeholder.com");
                placeholder.setCompany(device.getCompany());
                return userRepository.save(placeholder);
            });

            AccessLogsModel log = new AccessLogsModel();
            log.setUser(user);
            log.setDevice(device);
            log.setCompany(device.getCompany());
            log.setAction(inout == 0 ? AccessType.ENTRY : AccessType.EXIT);
            log.setCreatedAt(ZonedDateTime.parse(timestamp));

            accessLogsService.createLog(log);
            System.out.println("Device saved log " + sn);
        }
    }

    private void handleSendLog(JsonNode json) {
        String sn = json.get("sn").asText();
        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        JsonNode records = json.get("record");
        if (records == null || !records.isArray()) {
            System.err.println("Invalid log data from " + sn);
            return;
        }

        for (JsonNode record : records) {
            long userId = record.get("enrollid").asLong();
            int mode = record.get("mode").asInt(); // 0=fp, 1=card, 2=pwd
            int inout = record.get("inout").asInt(); // 0=in, 1=out
            String time = record.get("time").asText();

            UserModel user = userRepository.findById(userId).orElseGet(() -> {
                UserModel newUser = new UserModel();
                newUser.setId(userId);
                newUser.setName("User " + userId);
                newUser.setEmail("auto_" + userId + "@device.local");
                newUser.setCompany(device.getCompany());
                return userRepository.save(newUser);
            });

            AccessLogsModel log = new AccessLogsModel();
            log.setUser(user);
            log.setDevice(device);
            log.setCompany(device.getCompany());
            log.setAction(inout == 0 ? AccessType.ENTRY : AccessType.EXIT);
            log.setCreatedAt(ZonedDateTime.parse(time));

            accessLogsService.createLog(log);
        }

        // Enviar confirmación de recepción al dispositivo
        String response = String.format(
                "{\"ret\":\"sendlog\",\"result\":true,\"cloudtime\":\"%s\",\"access\":1}",
                ZonedDateTime.now()
        );
        sendCommand(response);

        System.out.println("✅ Received real-time access logs from " + sn);
    }

    private void handleLogDownload(JsonNode json, boolean isNewLogs) {
        String sn = json.get("sn").asText();
        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        JsonNode records = json.get("record");
        if (records == null || !records.isArray() || records.isEmpty()) {
            System.out.println("No " + (isNewLogs ? "new" : "all") + " logs from device " + sn);
            return;
        }

        for (JsonNode record : records) {
            Long userId = record.get("enrollid").asLong();
            String time = record.get("time").asText();
            int inout = record.get("inout").asInt();

            UserModel user = userRepository.findById(userId).orElseGet(() -> {
                UserModel placeholder = new UserModel();
                placeholder.setId(userId);
                placeholder.setName("User " + userId);
                placeholder.setEmail("unknown_" + userId + "@device.local");
                placeholder.setCompany(device.getCompany());
                return userRepository.save(placeholder);
            });

            AccessLogsModel log = new AccessLogsModel();
            log.setUser(user);
            log.setDevice(device);
            log.setCompany(device.getCompany());
            log.setAction(inout == 0 ? AccessType.ENTRY : AccessType.EXIT);
            log.setCreatedAt(ZonedDateTime.parse(time));

            accessLogsService.createLog(log);
        }

        // Confirmación para el siguiente paquete
        String cmd = isNewLogs ? "getnewlog" : "getalllog";
        sendCommand("{\"cmd\":\"" + cmd + "\",\"stn\":false}");

        System.out.println("✅ Stored " + (isNewLogs ? "new" : "all") + " logs from device " + sn);
    }


    private void handleNewUser(JsonNode json) {
        String sn = json.get("sn").asText();
        Long userId = json.get("enrollid").asLong();
        String name = json.has("name") ? json.get("name").asText() : "User " + userId;

        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        UserModel user = userRepository.findById(userId).orElse(new UserModel());
        user.setId(userId);
        user.setName(name);
        user.setEmail("user_" + userId + "@autocreated.com");
        user.setCompany(device.getCompany());

        userRepository.save(user);
        System.out.println("User synchronized from device: " + name + " (ID: " + userId + ")");
    }

    private void handleEvent(JsonNode json) {
        String sn = json.get("sn").asText();
        String eventType = json.get("event").asText();
        String timestamp = json.get("time").asText();

        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        AccessLogsModel log = new AccessLogsModel();
        log.setDevice(device);
        log.setCompany(device.getCompany());
        log.setCreatedAt(ZonedDateTime.parse(timestamp));
        log.setAction(AccessType.ENTRY);

        accessLogsService.createLog(log);
        System.out.println("Event received from device " + sn + ": " + eventType);
    }

    private void handleUserList(JsonNode json) {
        String sn = json.get("sn").asText();
        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        JsonNode records = json.get("record");
        if (records == null || !records.isArray()) {
            System.err.println("Invalid or empty user list from device: " + sn);
            return;
        }

        System.out.println("📋 Receiving user list from device " + sn + " (" + records.size() + " users)");

        for (JsonNode record : records) {
            Long userId = record.get("enrollid").asLong();
            String name = record.has("name") ? record.get("name").asText() : "User " + userId;

            UserModel user = userRepository.findById(userId).orElse(new UserModel());
            user.setId(userId);
            user.setName(name);
            user.setEmail("device_" + userId + "@autogenerated.com");
            user.setCompany(device.getCompany());

            userRepository.save(user);
        }

        System.out.println("User list from device " + sn + " saved successfully");
    }

    // =========================
    // Sending commands
    // =========================
    public void sendCommand(String cmdJson) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(cmdJson);
            System.out.println("Command sent: " + cmdJson);
        } else {
            System.out.println("There is no active connection to the device.");
        }
    }

    // Predefined commands
    /*
    public void requestAllLogs() {
        sendCommand("{\"cmd\":\"getalllog\"}");
    }
*/
    public void requestAllLogs() {
        sendCommand("{\"cmd\":\"getalllog\",\"stn\":true}");
    }

    public void openDoor() {
        sendCommand("{\"cmd\":\"opendoor\"}");
    }

    public void requestNewLogs() {
        sendCommand("{\"cmd\":\"getnewlog\",\"stn\":true}");
    }

    public void setUserInfo(Long enrollId, String name) {
        sendCommand("{\"cmd\":\"setuserinfo\",\"enrollid\":\"" + enrollId + "\",\"name\":\"" + name + "\"}");
    }

    public void sendUserToDevice(UserModel user) {
        String payload = String.format(
                "{\"cmd\":\"setuserinfo\",\"enrollid\":\"%d\",\"name\":\"%s\",\"password\":\"%s\",\"cardno\":\"%s\",\"privilege\":%d}",
                user.getId(),
                user.getName(),
                user.getPassword() != null ? user.getPassword() : "",
                0 // privilegio normal (ajusta según tu caso)
        );

        sendCommand(payload);
    }

    public void deleteUserFromDevice(Long userId) {
        String payload = String.format("{\"cmd\":\"deleteuser\",\"enrollid\":\"%d\"}", userId);
        sendCommand(payload);
    }

    public void clearAllUsers() {
        sendCommand("{\"cmd\":\"clearuser\"}");
    }

    public void requestUserList() {
        sendCommand("{\"cmd\":\"getuserlist\"}");
    }
}
