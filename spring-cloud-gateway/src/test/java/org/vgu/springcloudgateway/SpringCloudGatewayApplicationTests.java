package org.vgu.springcloudgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic context load test for Spring Cloud Gateway
 * This test verifies that the application context loads successfully
 */
@SpringBootTest
@ActiveProfiles("test")
class SpringCloudGatewayApplicationTests {

    @Test
    void contextLoads() {
        // Test that application context loads without errors
    }

}
