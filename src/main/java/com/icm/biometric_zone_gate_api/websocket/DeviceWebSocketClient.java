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
    private String host; // para reconexión

    // =========================
    // Conectar al dispositivo
    // =========================
    public void connect(String host) {
        this.host = host;
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(this, new URI("ws://" + host + ":7788"));
            System.out.println("Conectado al dispositivo en: " + host);
        } catch (Exception e) {
            System.err.println("Error al conectar con el dispositivo, reintentando en 5s...");
            scheduleReconnect();
        }
    }

    // =========================
    // Procesar mensajes recibidos
    // =========================
    @OnMessage
    public void onMessage(String message) {
        System.out.println("Recibido: " + message);

        try {
            JsonNode json = objectMapper.readTree(message);
            String cmd = json.get("cmd").asText();

            switch (cmd) {
                case "reg":
                    handleRegister(json);
                    break;
                case "sendlog":
                    handleLogs(json);
                    break;
                case "senduser":
                    handleNewUser(json);
                    break;
                case "event":
                    handleEvent(json);
                    break;
                default:
                    System.out.println("Comando no manejado: " + cmd);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // Manejo de registro de dispositivo
    // =========================
    private void handleRegister(JsonNode json) {
        String sn = json.get("sn").asText();
        String language = json.has("language") ? json.get("language").asText() : "es";

        DeviceModel device = deviceRepository.findBySn(sn).orElse(new DeviceModel());
        device.setSn(sn);
        device.setLanguage(language);
        device.setHost(host);
        device.setPort("7788");

        // Asignar compañía por defecto (ejemplo ID = 1)
        CompanyModel defaultCompany = companyRepository.findById(1L).orElseThrow();
        device.setCompany(defaultCompany);

        deviceRepository.save(device);
        System.out.println("Dispositivo registrado/actualizado: " + sn);
    }

    // =========================
    // Manejo de logs recibidos
    // =========================
    private void handleLogs(JsonNode json) {
        String sn = json.get("sn").asText();
        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        for (JsonNode record : json.get("record")) {
            Long userId = record.get("enrollid").asLong();
            int inout = record.get("inout").asInt();
            String timestamp = record.get("time").asText();

            // Buscar usuario o crear placeholder
            UserModel user = userRepository.findById(userId).orElseGet(() -> {
                UserModel placeholder = new UserModel();
                placeholder.setId(userId); // usar enrollid como ID
                placeholder.setName("Usuario " + userId);
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
            System.out.println("Log guardado de dispositivo " + sn);
        }
    }

    // =========================
    // Manejo de usuarios enviados desde el dispositivo
    // =========================
    private void handleNewUser(JsonNode json) {
        String sn = json.get("sn").asText();
        Long userId = json.get("enrollid").asLong();
        String name = json.has("name") ? json.get("name").asText() : "Usuario " + userId;

        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        UserModel user = userRepository.findById(userId).orElse(new UserModel());
        user.setId(userId);
        user.setName(name);
        user.setEmail("usuario_" + userId + "@autocreated.com");
        user.setCompany(device.getCompany());

        userRepository.save(user);
        System.out.println("Usuario sincronizado desde dispositivo: " + name + " (ID: " + userId + ")");
    }

    // =========================
    // Manejo de eventos especiales (ej: apertura de puerta)
    // =========================
    private void handleEvent(JsonNode json) {
        String sn = json.get("sn").asText();
        String eventType = json.get("event").asText();
        String timestamp = json.get("time").asText();

        DeviceModel device = deviceRepository.findBySn(sn).orElseThrow();

        AccessLogsModel log = new AccessLogsModel();
        log.setDevice(device);
        log.setCompany(device.getCompany());
        log.setCreatedAt(ZonedDateTime.parse(timestamp));

        // Puedes mapear eventos a ENTRY/EXIT o extender AccessType con EVENT
        log.setAction(AccessType.ENTRY);

        accessLogsService.createLog(log);
        System.out.println("Evento recibido de dispositivo " + sn + ": " + eventType);
    }

    // =========================
    // Abrir conexión
    // =========================
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("🔗 Conexión abierta con el dispositivo.");
    }

    // =========================
    // Manejar desconexión y reconexión
    // =========================
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Conexión cerrada: " + reason);
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // espera 5 segundos
                connect(host);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    // =========================
    // Enviar comandos al dispositivo
    // =========================
    public void sendCommand(String cmdJson) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(cmdJson);
            System.out.println("Enviado comando: " + cmdJson);
        } else {
            System.out.println("No hay conexión activa con el dispositivo.");
        }
    }

    // Helpers para comandos comunes
    public void requestAllLogs() {
        sendCommand("{\"cmd\":\"getalllog\"}");
    }

    public void openDoor() {
        sendCommand("{\"cmd\":\"opendoor\"}");
    }

    public void setUserInfo(Long enrollId, String name) {
        sendCommand("{\"cmd\":\"setuserinfo\",\"enrollid\":\"" + enrollId + "\",\"name\":\"" + name + "\"}");
    }
}
