package com.icm.biometric_zone_gate_api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.websocket.handlers.*;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DeviceMessageHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeviceService deviceService;

    private final DeviceSessionManager deviceSessionManager;
    private final RegisterHandler registerHandler;
    private final SendLogHandler sendLogHandler;
    private final SendUserHandler sendUserHandler;
    private final GetUserListResponseHandler getUserListResponseHandler;
    private final GetUserInfoResponseHandler getUserInfoResponseHandler;
    private final SetUserInfoResponseHandler setUserInfoResponseHandler;
    private final DeleteUserResponseHandler deleteUserResponseHandler;
    private final GetUserNameResponseHandler getUserNameResponseHandler;
    private final SetUserNameResponseHandler setUserNameResponseHandler;
    private final EnableUserResponseHandler enableUserResponseHandler;
    private final CleanUserResponseHandler cleanUserResponseHandler;
    private final InitSystemResponseHandler initSystemResponseHandler;
    private final CleanAdminResponseHandler cleanAdminResponseHandler;
    private final SetTimeResponseHandler setTimeResponseHandler;

    public void handle(String message, WebSocketSession session) {
        try {
            System.out.println("Mensaje recibido del dispositivo " + session.getId() + ": " + message);

            JsonNode json = objectMapper.readTree(message);
            //String cmd = json.path("cmd").asText("unknown");
            String cmd = json.has("cmd") ? json.get("cmd").asText() :
                    json.has("ret") ? json.get("ret").asText() :
                            "unknown";

            switch (cmd) {

                case "reg" -> registerHandler.handleRegister(json, session);

                case "sendlog" -> sendLogHandler.handleSendLog(json, session);

                case "senduser" -> sendUserHandler.handleSendUser(json, session);

                case "getuserlist" -> getUserListResponseHandler.handleGetUserListResponse(json, session);

                case "getuserinfo" -> getUserInfoResponseHandler.handleGetUserInfoResponse(json);

                case "setuserinfo" -> setUserInfoResponseHandler.handleSetUserInfoResponse(json);

                case "deleteuser" -> deleteUserResponseHandler.handleDeleteUserResponse(json);

                case "getusername" -> getUserNameResponseHandler.handleGetUserNameResponse(json);

                case "setusername" -> setUserNameResponseHandler.handleSetUserNameResponse(json);

                case "enableuser" -> enableUserResponseHandler.handleEnableUserResponse(json);

                case "cleanuser" -> cleanUserResponseHandler.handleCleanUserResponse(json);

                case "initsys" -> initSystemResponseHandler.handleInitSystemResponse(json);

                case "cleanadmin" -> cleanAdminResponseHandler.handleCleanAdminResponse(json);

                case "settime" -> setTimeResponseHandler.handleSetTimeResponse(json);

                default -> {
                    System.out.println("Unknown command: " + cmd);
                    session.sendMessage(new TextMessage("{\"status\": \"error\", \"msg\": \"Unknown command\"}"));
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing message from " + session.getId() + ": " + e.getMessage());
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        System.out.println("Device disconnected: " + session.getId());

        System.out.println("Device disconnected: " + session.getId());
        String sn = (String) session.getAttributes().get("sn");
        if (sn != null) {
            deviceSessionManager.removeSession(sn, session); // <-- quitar sesión
            deviceService.getDeviceBySn(sn).ifPresent(device -> {
                deviceService.updateDeviceStatus(device.getId(), DeviceStatus.DISCONNECTED);
                System.out.println("Dispositivo marcado como DESCONECTADO: " + sn);
            });
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Device connected: " + session.getId());
        // SN se asignará en handleRegister cuando llegue el mensaje
    }
}