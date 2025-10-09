package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import com.icm.biometric_zone_gate_api.services.UserCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/credentials")
@RequiredArgsConstructor
public class UserCredentialController {

    private final UserCredentialService userCredentialService;

    @GetMapping
    public ResponseEntity<List<UserCredentialModel>> getAllCredentials() {
        return ResponseEntity.ok(userCredentialService.getAllCredentials());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<UserCredentialModel>> getAllCredentialsPaged(Pageable pageable) {
        return ResponseEntity.ok(userCredentialService.getAllCredentials(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserCredentialModel> getCredentialById(@PathVariable Long id) {
        return userCredentialService.getCredentialById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserCredentialModel>> getCredentialsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userCredentialService.getCredentialsByUserId(userId));
    }

    @GetMapping("/user/{userId}/page")
    public ResponseEntity<Page<UserCredentialModel>> getCredentialsByUserIdPaged(
            @PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(userCredentialService.getCredentialsByUserId(userId, pageable));
    }

    @PostMapping
    public ResponseEntity<UserCredentialModel> createCredential(@RequestBody UserCredentialModel credential) {
        return ResponseEntity.ok(userCredentialService.createCredential(credential));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserCredentialModel> updateCredential(
            @PathVariable Long id, @RequestBody UserCredentialModel updatedCredential) {
        return userCredentialService.updateCredential(id, updatedCredential)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCredential(@PathVariable Long id) {
        return userCredentialService.deleteCredential(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
