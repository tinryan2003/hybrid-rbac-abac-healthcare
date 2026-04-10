package org.vgu.authorizationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * PIP - Resource attributes for invoice.
 */
@FeignClient(name = "billing-service", url = "${services.billing-service.url}")
public interface BillingPipClient {

    @GetMapping("/billing/invoices/pip/resource/{resourceId}")
    ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable("resourceId") Long resourceId);
}
