package org.vgu.patientservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Patient Service - Hospital Management System
 * Manages patient demographics, medical history, and allergy records
 * 
 * Port: 8085
 * Database: hospital_patients
 * 
 * Key Features:
 * - Patient profile management (linked with Keycloak)
 * - Medical history tracking (vitals, prescriptions)
 * - Patient allergy records
 * - ABAC attributes (hospital_id for multi-hospital support)
 * - Integration with Keycloak for authentication
 */
@SpringBootApplication
public class PatientServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientServiceApplication.class, args);
	}

}
