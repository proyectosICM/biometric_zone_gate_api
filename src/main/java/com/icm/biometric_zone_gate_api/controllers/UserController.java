package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.dto.UserDTO;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    // GET /api/v1/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // GET /api/v1/users/ids
    @GetMapping("/ids")
    public ResponseEntity<List<String>> getAllUserIds() {
        List<String> ids = userService.getAllUsers().stream()
                .map(UserDTO::getUserId)
                .toList();
        return ResponseEntity.ok(ids);
    }

    // GET /api/v1/users
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // POST /api/v1/users/sync
    @PostMapping("/sync")
    public ResponseEntity<List<UserModel>> syncUsers(@RequestBody List<UserDTO> users) {
        return ResponseEntity.ok(userService.syncUsers(users));
    }

    // POST /api/v1/users
    @PostMapping
    public ResponseEntity<UserModel> createUser(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.createUser(userDTO));
    }

    // PUT /api/v1/users/{id}
    @PutMapping("/{id}")
    public ResponseEntity<UserModel> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    // DELETE /api/v1/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}

