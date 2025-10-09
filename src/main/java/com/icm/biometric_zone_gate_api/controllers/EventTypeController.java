package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.models.EventTypeModel;
import com.icm.biometric_zone_gate_api.services.EventTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-types")
@RequiredArgsConstructor
    public class EventTypeController {

    private final EventTypeService eventTypeService;

    @GetMapping
    public ResponseEntity<List<EventTypeModel>> getAllEventTypes() {
        return ResponseEntity.ok(eventTypeService.getAllEventTypes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventTypeById(@PathVariable Long id) {
        return eventTypeService.getEventTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<?> getEventTypeByCode(@PathVariable int code) {
        return eventTypeService.getEventTypeByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createEventType(@RequestBody EventTypeModel eventType) {
        try {
            EventTypeModel saved = eventTypeService.createEventType(eventType);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEventType(@PathVariable Long id, @RequestBody EventTypeModel updated) {
        try {
            EventTypeModel result = eventTypeService.updateEventType(id, updated);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEventType(@PathVariable Long id) {
        try {
            eventTypeService.deleteEventType(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearAllEventTypes() {
        eventTypeService.clearAllEventTypes();
        return ResponseEntity.ok("Todos los tipos de evento fueron eliminados.");
    }
}
