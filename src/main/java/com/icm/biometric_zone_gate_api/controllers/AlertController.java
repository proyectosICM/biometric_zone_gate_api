package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.models.AlertModel;
import com.icm.biometric_zone_gate_api.services.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // ------- GET by ID -------
    @GetMapping("/{id}")
    public ResponseEntity<AlertModel> getById(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getById(id));
    }

    // ------- LIST (no paged) -------
    @GetMapping
    public List<AlertModel> findAll() {
        return alertService.findAll();
    }

    // ------- LIST (paged) -------
    @GetMapping("page")
    public Page<AlertModel> findAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "DESC") Sort.Direction dir
    ) {
        Pageable pageable = buildPageable(page, size, sort, dir);
        return alertService.findAll(pageable);
    }

    // ------- LIST by company (no paged) -------
    @GetMapping("/company/{companyId}")
    public List<AlertModel> findByCompany(@PathVariable Long companyId) {
        return alertService.findByCompany(companyId);
    }

    // ------- LIST by company (paged) -------
    @GetMapping("/company/{companyId}/paged")
    public Page<AlertModel> findByCompanyPaged(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "DESC") Sort.Direction dir
    ) {
        Pageable pageable = buildPageable(page, size, sort, dir);
        return alertService.findByCompany(companyId, pageable);
    }

    // ------- CREATE -------
    @PostMapping
    public ResponseEntity<AlertModel> create(@Valid @RequestBody AlertModel body) {
        AlertModel created = alertService.save(body);
        return ResponseEntity.created(URI.create("/api/alerts/" + created.getId())).body(created);
    }

    // ------- UPDATE -------
    @PutMapping("/{id}")
    public ResponseEntity<AlertModel> update(
            @PathVariable Long id,
            @Valid @RequestBody AlertModel body
    ) {
        return ResponseEntity.ok(alertService.update(id, body));
    }

    // ------- DELETE -------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        alertService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ------- Helpers -------
    private Pageable buildPageable(int page, int size, String sort, Sort.Direction dir) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return PageRequest.of(page, size, Sort.by(dir, sort));
    }

    // ------- Error mapping -------
    @ExceptionHandler(AlertService.NotFoundException.class)
    public ResponseEntity<String> handleNotFound(AlertService.NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
