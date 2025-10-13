package com.icm.biometric_zone_gate_api.websocket.utils;

import com.fasterxml.jackson.databind.JsonNode;

public class DeviceValidator {

    /**
     * Validates that the devinfo JsonNode contains all required and correct fields.
     * @param devinfo JsonNode with the device information
     * @return true if valid, false if any field is missing or incorrect
     */
    public static boolean validateDevInfo(JsonNode devinfo) {
        if (devinfo == null) return false;

        String[] requiredStringFields = {"modelname", "fpalgo", "firmware", "time"};
        String[] requiredIntFields = {
                "usersize", "fpsize", "cardsize", "pwdsize", "logsize",
                "useduser", "usedfp", "usedcard", "usedpwd", "usedlog", "usednewlog"
        };

        for (String field : requiredStringFields) {
            String value = devinfo.path(field).asText(null);
            if (value == null || value.isEmpty()) {
                System.err.println("Campo crítico faltante o vacío: " + field);
                return false;
            }
        }

        for (String field : requiredIntFields) {
            if (!devinfo.has(field) || !devinfo.get(field).canConvertToInt()) {
                System.err.println("Campo crítico faltante o inválido: " + field);
                return false;
            }
        }

        return true;
    }
}
