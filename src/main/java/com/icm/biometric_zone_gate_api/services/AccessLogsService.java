package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.AccessLogsRepository;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessLogsService {

    private final AccessLogsRepository accessLogsRepository;

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
