package com.icm.biometric_zone_gate_api.services.impl;

import com.icm.biometric_zone_gate_api.dto.DeviceDTO;
import com.icm.biometric_zone_gate_api.dto.DeviceSettingDTO;
import com.icm.biometric_zone_gate_api.mappers.DeviceMapper;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


import org.springframework.http.*;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
    private final DeviceRepository deviceRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    @Override
    public Optional<DeviceDTO> findById(Long id) {
        return deviceRepository.findById(id)
                .map(DeviceMapper::toDTO);
    }

    @Override
    public DeviceDTO save(DeviceDTO deviceDTO) {
        return DeviceMapper.toDTO(deviceRepository.save(DeviceMapper.toEntity(deviceDTO)));
    }

    @Override
    public DeviceDTO update(DeviceDTO deviceDTO) {
        if (deviceDTO.getId() == null || !deviceRepository.existsById(deviceDTO.getId())) {
            throw new IllegalArgumentException("Device ID must be provided and exist for update.");
        }
        return DeviceMapper.toDTO(deviceRepository.save(DeviceMapper.toEntity(deviceDTO)));
    }

    @Override
    public void deleteById(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new IllegalArgumentException("Device not found with id: " + id);
        }
        deviceRepository.deleteById(id);
    }

    public String sendDeviceSetting(Long deviceId, DeviceSettingDTO settingDTO) {
        DeviceModel device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado"));

        // Construir payload del protocolo BS
        Map<String, Object> payload = new HashMap<>();
        payload.put("cmd_code", "SET_DEVICE_SETTING");
        payload.put("dev_id", device.getId());
        payload.put("token", device.getToken()); // o calcula md5(dev_id + clave) si es dinámico
        payload.put("trans_id", UUID.randomUUID().toString());
        payload.put("settings", settingDTO);

        try {
            String url = "http://" + settingDTO.getServerHost() + ":" + settingDTO.getServerPort() + "/receive_cmd"; // según tu dispositivo

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            return response.getBody(); // respuesta cruda del dispositivo
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar SET_DEVICE_SETTING: " + e.getMessage());
        }
    }
}
