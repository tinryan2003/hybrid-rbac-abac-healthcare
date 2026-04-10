package org.vgu.authorizationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * PIP - Resource attributes for prescription.
 */
@FeignClient(name = "pharmacy-service", url = "${services.pharmacy-service.url}")
public interface PharmacyPipClient {

    @GetMapping("/pharmacy/prescriptions/pip/resource/{resourceId}")
    ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable("resourceId") Long resourceId);
}
