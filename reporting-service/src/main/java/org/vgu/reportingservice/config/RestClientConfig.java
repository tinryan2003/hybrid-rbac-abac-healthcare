package org.vgu.reportingservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${services.audit-service.url}")
    private String auditServiceUrl;

    @Value("${services.patient-service.url}")
    private String patientServiceUrl;

    @Value("${services.appointment-service.url}")
    private String appointmentServiceUrl;

    @Value("${services.billing-service.url}")
    private String billingServiceUrl;

    @Value("${services.lab-service.url}")
    private String labServiceUrl;

    @Value("${services.pharmacy-service.url}")
    private String pharmacyServiceUrl;

    @Bean("auditRestClient")
    public RestClient auditRestClient() {
        return RestClient.builder().baseUrl(auditServiceUrl).build();
    }

    @Bean("patientRestClient")
    public RestClient patientRestClient() {
        return RestClient.builder().baseUrl(patientServiceUrl).build();
    }

    @Bean("appointmentRestClient")
    public RestClient appointmentRestClient() {
        return RestClient.builder().baseUrl(appointmentServiceUrl).build();
    }

    @Bean("billingRestClient")
    public RestClient billingRestClient() {
        return RestClient.builder().baseUrl(billingServiceUrl).build();
    }

    @Bean("labRestClient")
    public RestClient labRestClient() {
        return RestClient.builder().baseUrl(labServiceUrl).build();
    }

    @Bean("pharmacyRestClient")
    public RestClient pharmacyRestClient() {
        return RestClient.builder().baseUrl(pharmacyServiceUrl).build();
    }
}
