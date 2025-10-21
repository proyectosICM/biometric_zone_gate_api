package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.models.EventTypeModel;
import com.icm.biometric_zone_gate_api.repositories.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventTypeService {
    private final EventTypeRepository eventTypeRepository;

    private static final int MAX_EVENT_TYPES = 17;

    public List<EventTypeModel> getAllEventTypes() {
        return eventTypeRepository.findAll();
    }

    public Page<EventTypeModel> getAllEventTypes(Pageable pageable) {
        return eventTypeRepository.findAll(pageable);
    }

    public Optional<EventTypeModel> getEventTypeById(Long id) {
        return eventTypeRepository.findById(id);
    }

    public EventTypeModel createEventType(EventTypeModel eventType) {
        long count = eventTypeRepository.count();
        if (count >= MAX_EVENT_TYPES) {
            throw new IllegalStateException("Se alcanzó el límite máximo de " + MAX_EVENT_TYPES + " tipos de evento permitidos.");
        }

        // Verificar que no se repita el código
        if (eventTypeRepository.existsByCode(eventType.getCode())) {
            throw new IllegalArgumentException("El código de evento " + eventType.getCode() + " ya existe.");
        }

        return eventTypeRepository.save(eventType);
    }

    public EventTypeModel updateEventType(Long id, EventTypeModel updatedEventType) {
        EventTypeModel existing = eventTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el tipo de evento con ID " + id));

        existing.setCode(updatedEventType.getCode());
        existing.setName(updatedEventType.getName());
        existing.setDescription(updatedEventType.getDescription());

        return eventTypeRepository.save(existing);
    }

    public void deleteEventType(Long id) {
        if (!eventTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("No existe un tipo de evento con ID " + id);
        }
        eventTypeRepository.deleteById(id);
    }

    public void clearAllEventTypes() {
        eventTypeRepository.deleteAll();
    }

    public Optional<EventTypeModel> getEventTypeByCode(int code) {
        return eventTypeRepository.findByCode(code);
    }

}
