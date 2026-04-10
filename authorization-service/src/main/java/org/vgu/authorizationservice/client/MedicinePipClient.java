package org.vgu.authorizationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * PIP - Resource attributes for medicine (pharmacy inventory).
 * Returns hospital_id, sensitivity_level, controlled_substance, requires_prescription.
 */
@FeignClient(name = "medicine-pip-client", url = "${services.pharmacy-service.url}")
public interface MedicinePipClient {

    @GetMapping("/pharmacy/medicines/pip/resource/{resourceId}")
    ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable("resourceId") Long resourceId);
}
