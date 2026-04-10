package org.vgu.authorizationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * PIP - Resource attributes for lab_order.
 */
@FeignClient(name = "lab-service", url = "${services.lab-service.url}")
public interface LabPipClient {

    @GetMapping("/lab/orders/pip/resource/{resourceId}")
    ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable("resourceId") Long resourceId);
}
