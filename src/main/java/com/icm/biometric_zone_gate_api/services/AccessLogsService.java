package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.dto.AccessLogEppPatchDTO;
import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.AccessLogsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessLogsService {

    private final AccessLogsRepository accessLogsRepository;

    private static final int MEDIUMTEXT_MAX_BYTES = 16_777_215;

    private static final long MAX_EPP_PATCH_MINUTES = 10;

    public List<AccessLogsModel> getAllLogs() {
        return accessLogsRepository.findAll();
    }

    public Page<AccessLogsModel> getAllLogs(Pageable pageable) {
        return accessLogsRepository.findAll(pageable);
    }

    public Optional<AccessLogsModel> getLogById(Long id) {
        return accessLogsRepository.findById(id);
    }

    public AccessLogsModel createLog(AccessLogsModel log) {
        return accessLogsRepository.save(log);
    }

    public Optional<AccessLogsModel> updateObservation(Long id, String observation) {
        return accessLogsRepository.findById(id).map(log -> {
            log.setObservation(observation);
            return accessLogsRepository.save(log);
        });
    }

    /*
    @Transactional
    public Optional<AccessLogsModel> patchEpp(Long id, AccessLogEppPatchDTO dto) {
        return accessLogsRepository.findById(id).map(log -> {

            // 1) correctEpp (si viene null, no tocar)
            if (dto.getCorrectEpp() != null) {
                log.setCorrectEpp(dto.getCorrectEpp());
            }

            // 2) entryEppPhotoB64 (si viene null, no tocar; si es "", borrar)
            if (dto.getEntryEppPhotoB64() != null) {
                String raw = dto.getEntryEppPhotoB64().trim();
                if (raw.isEmpty()) {
                    // borrar foto
                    log.setEntryEppPhotoB64(null);
                } else {
                    // normalizar: quitar prefijo dataURL si viene
                    String normalized = stripDataUrlPrefix(raw);

                    // validación básica de Base64 y tamaño
                    validateBase64Approx(normalized);

                    log.setEntryEppPhotoB64(normalized);
                }
            }

            return accessLogsRepository.save(log);
        });
    }
     */

    @Transactional
    public Optional<AccessLogsModel> patchEpp(Long id, AccessLogEppPatchDTO dto) {
        return accessLogsRepository.findById(id).map(log -> {

            ZonedDateTime now = ZonedDateTime.now();
            // Si quieres basarte en la hora de entrada:
            if (log.getEntryTime() != null) {
                long diffMinutes = Duration.between(log.getEntryTime(), now).toMinutes();
                if (diffMinutes > MAX_EPP_PATCH_MINUTES) {
                    // aquí puedes lanzar excepción o simplemente no guardar cambios
                    throw new IllegalStateException(
                            "No se puede actualizar EPP de un registro con más de " + MAX_EPP_PATCH_MINUTES + " minutos."
                    );
                }
            }

            // Opcional: no permitir cambios si ya está cerrado hace rato
            if (log.getExitTime() != null) {
                long diffMinutesSinceExit = Duration.between(log.getExitTime(), now).toMinutes();
                if (diffMinutesSinceExit > 0) {
                    throw new IllegalStateException("No se puede actualizar EPP de un registro ya cerrado.");
                }
            }

            // 1) correctEpp (si viene null, no tocar)
            if (dto.getCorrectEpp() != null) {
                log.setCorrectEpp(dto.getCorrectEpp());
            }

            // 2) entryEppPhotoB64 (si viene null, no tocar; si es "", borrar)
            if (dto.getEntryEppPhotoB64() != null) {
                String raw = dto.getEntryEppPhotoB64().trim();
                if (raw.isEmpty()) {
                    log.setEntryEppPhotoB64(null);
                } else {
                    String normalized = stripDataUrlPrefix(raw);
                    validateBase64Approx(normalized);

                    // Opcional: si ya tiene foto, podrías decidir no sobreescribir
                    if (log.getEntryEppPhotoB64() == null) {
                        log.setEntryEppPhotoB64(normalized);
                    } else {
                        // o log.warn / ignorar / permitir overwrite según lo que tú quieras
                        log.setEntryEppPhotoB64(normalized);
                    }
                }
            }

            return accessLogsRepository.save(log);
        });
    }

    /** Quita "data:image/jpeg;base64," si viene con prefijo. */
    private String stripDataUrlPrefix(String s) {
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma >= 0) {
            return s.substring(comma + 1);
        }
        return s;
    }

    /**
     * Valida que el string sea Base64 y que no exceda MEDIUMTEXT.
     * (Decodifica solo para validar; si te preocupa performance, puedes
     *  estimar el tamaño: bytes ≈ base64.length * 3 / 4)
     */
    private void validateBase64Approx(String base64) {
        // tamaño estimado de texto en la columna (el propio Base64):
        if (base64.length() > MEDIUMTEXT_MAX_BYTES) {
            throw new IllegalArgumentException("La foto en Base64 excede el tamaño permitido (MEDIUMTEXT).");
        }

        // validación de formato Base64 (lanza IllegalArgumentException si no es válido)
        try {
            Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Formato Base64 inválido para la foto.", ex);
        }
    }

    public boolean deleteLog(Long id) {
        if (accessLogsRepository.existsById(id)) {
            accessLogsRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<AccessLogsModel> findLogByUserDeviceAndTime(Long userId, Long deviceId, ZonedDateTime time) {
        return accessLogsRepository.findLogByUserDeviceAndTime(userId, deviceId, time);
    }

    public Optional<AccessLogsModel> findLastClosedLogByUserDevice(Long userId, Long deviceId, ZonedDateTime time) {
        return accessLogsRepository.findLastClosedLogByUserDevice(userId, deviceId, time);
    }

    public List<AccessLogsModel> getLogsByUser(Long userId) {
        return accessLogsRepository.findByUserId(userId);
    }

    public Page<AccessLogsModel> getLogsByUser(Long userId, Pageable pageable) {
        return accessLogsRepository.findByUserId(userId, pageable);
    }

    public List<AccessLogsModel> getLogsByDevice(Long deviceId) {
        return accessLogsRepository.findByDeviceId(deviceId);
    }

    public Page<AccessLogsModel> getLogsByDevice(Long deviceId, Pageable pageable) {
        return accessLogsRepository.findByDeviceId(deviceId, pageable);
    }

    public List<AccessLogsModel> getLogsByCompany(Long companyId) {
        return accessLogsRepository.findByCompanyId(companyId);
    }

    public Page<AccessLogsModel> getLogsByCompany(Long companyId, Pageable pageable) {
        return accessLogsRepository.findByCompanyId(companyId, pageable);
    }

    public List<AccessLogsModel> getLogsByAction(AccessType action) {
        return accessLogsRepository.findByAction(action);
    }

    public Page<AccessLogsModel> getLogsByAction(AccessType action, Pageable pageable) {
        return accessLogsRepository.findByAction(action, pageable);
    }

    @Transactional
    public void autoCloseOldOpenLogs(int minutes, String systemObservation) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime limit = now.minusMinutes(minutes);

        List<AccessLogsModel> staleLogs = accessLogsRepository.findOpenLogsOlderThan(limit);
        if (staleLogs.isEmpty()) return;

        for (AccessLogsModel log : staleLogs) {
            // Para consistencia: salida exactamente 30 minutos después de la entrada
            ZonedDateTime exitTime = log.getEntryTime().plusMinutes(minutes);
            if (exitTime.isAfter(now)) {
                // Por si el scheduler corre muy seguido y agarra uno “justo al límite”
                exitTime = now;
            }

            long durationSeconds = Duration.between(log.getEntryTime(), exitTime).getSeconds();

            log.setExitTime(exitTime);
            log.setDurationSeconds(durationSeconds);
            log.setAction(AccessType.EXIT);

            // Mantener el mismo auth mode de entrada, o setear algo especial si tienes
            if (log.getExitAuthMode() == null) {
                log.setExitAuthMode(log.getEntryAuthMode());
            }

            // Aquí asumo que el campo se llama observation / observacion / notes
            log.setObservation("cerrado por el sistema");

            // Si tu createLog() hace save(), puedes usarlo. Si no, usa repository.save()
            accessLogsRepository.save(log);
        }
    }

    public long countLogsByDeviceAndDay(Long deviceId, LocalDate date) {
        ZonedDateTime startOfDay = date.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());
        return accessLogsRepository.countByDeviceAndDay(deviceId, startOfDay, endOfDay);
    }


    public List<AccessLogsModel> getLatest4LogsByDeviceToday(Long deviceId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime startOfDay = now.toLocalDate().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = now.toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());

        PageRequest top4 = PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "entryTime"));

        return accessLogsRepository
                .findByDeviceIdAndEntryTimeBetween(deviceId, startOfDay, endOfDay, top4)
                .getContent();
    }

    public Optional<AccessLogsModel> getOpenLogForUserDevice(UserModel user, DeviceModel device) {
        return accessLogsRepository.findOpenLogByUserAndDevice(user.getId(), device.getId());
    }

    public byte[] exportByDeviceBetween(Long deviceId, ZonedDateTime from, ZonedDateTime to) {
        List<AccessLogsModel> logs = accessLogsRepository
                .findByDeviceIdAndCreatedAtBetween(deviceId, from, to);
        return buildExcel(logs, "Logs por Dispositivo");
    }

    public byte[] exportByCompanyBetween(Long companyId, ZonedDateTime from, ZonedDateTime to) {
        List<AccessLogsModel> logs = accessLogsRepository
                .findByCompanyIdAndCreatedAtBetween(companyId, from, to);
        return buildExcel(logs, "Logs por Empresa");
    }

    private byte[] buildExcel(List<AccessLogsModel> logs, String sheetName) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            // estilos simples
            CellStyle header = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            header.setFont(headerFont);

            int r = 0;
            Row h = sheet.createRow(r++);
            String[] cols = {"ID","Usuario","Dispositivo","Empresa","Evento",
                    "Entrada","Salida","Duración(s)","EPP Correcto","Éxito","Observación","Creado"};
            for (int i=0;i<cols.length;i++){
                Cell c = h.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(header);
            }

            for (AccessLogsModel log : logs) {
                Row row = sheet.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(log.getId() != null ? log.getId() : 0);
                row.createCell(c++).setCellValue(log.getUser() != null ? nullSafe(log.getUser().getName()) : "—");
                row.createCell(c++).setCellValue(log.getDevice() != null ? nullSafe(log.getDevice().getName()) : "—");
                row.createCell(c++).setCellValue(log.getCompany() != null ? nullSafe(log.getCompany().getName()) : "—");
                row.createCell(c++).setCellValue(log.getEventType() != null ? nullSafe(log.getEventType().getName()) : "—");
                row.createCell(c++).setCellValue(log.getEntryTime() != null ? log.getEntryTime().toString() : "—");
                row.createCell(c++).setCellValue(log.getExitTime() != null ? log.getExitTime().toString() : "—");
                row.createCell(c++).setCellValue(log.getDurationSeconds() != null ? log.getDurationSeconds() : 0);
                row.createCell(c++).setCellValue(Boolean.TRUE.equals(log.getCorrectEpp()) ? "Sí" : "No");
                row.createCell(c++).setCellValue(Boolean.TRUE.equals(log.getSuccess()) ? "✔" : "✖");
                row.createCell(c++).setCellValue(nullSafe(log.getObservation()));
                row.createCell(c).setCellValue(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "—");
            }

            for (int i=0;i<cols.length;i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error creando Excel de logs", e);
        }
    }

    private String nullSafe(String s){ return s == null ? "—" : s; }
}
