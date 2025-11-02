package com.icm.biometric_zone_gate_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BiometricZoneGateApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BiometricZoneGateApiApplication.class, args);
	}

}
