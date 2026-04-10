package org.vgu.pharmacyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pharmacy Service - Hospital Management System
 * Medicine catalog, prescriptions, prescription items, inventory transactions
 *
 * Port: 8095
 * Database: hospital_pharmacy
 * Keycloak realm: hospital-realm
 */
@SpringBootApplication
public class PharmacyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PharmacyServiceApplication.class, args);
	}

}
