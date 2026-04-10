package org.vgu.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * User Service - Hospital Management System
 * Manages hospital staff (doctors, nurses, admins, etc.) with ABAC attributes
 * 
 * Port: 8090
 * Database: hospital_users
 * 
 * Key Features:
 * - User profile management (linked with Keycloak)
 * - Doctor, Nurse, Admin, Lab Tech, Pharmacist management
 * - Department management
 * - ABAC attribute from Keycloak: hospital_id only (username, email, firstName, lastName are standard)
 * - Integration with Keycloak for authentication
 */
@SpringBootApplication
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
