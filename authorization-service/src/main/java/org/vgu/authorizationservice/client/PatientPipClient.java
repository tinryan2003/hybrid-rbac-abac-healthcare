package org.vgu.authorizationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * PIP - Resource attributes for patient / patient_record.
 */
@FeignClient(name = "patient-service", url = "${services.patient-service.url}")
public interface PatientPipClient {

    @GetMapping("/patients/pip/resource/{resourceId}")
    ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable("resourceId") Long resourceId);
}
