package org.vgu.authorizationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * PIP - Resource attributes for appointment.
 */
@FeignClient(name = "appointment-service", url = "${services.appointment-service.url}")
public interface AppointmentPipClient {

    @GetMapping("/api/appointments/pip/resource/{resourceId}")
    ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable("resourceId") Long resourceId);
}
