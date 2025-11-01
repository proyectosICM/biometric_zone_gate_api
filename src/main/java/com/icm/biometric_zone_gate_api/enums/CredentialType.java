package com.icm.biometric_zone_gate_api.enums;

public enum CredentialType {
    FINGERPRINT,   // 0-9 (backup) y mode=0
    PASSWORD,      // 10 (backup) y mode=2
    CARD,          // 11 (backup) y mode=1
    PHOTO,         // 50 (backup) y mode=8
    UNKNOWN
}
