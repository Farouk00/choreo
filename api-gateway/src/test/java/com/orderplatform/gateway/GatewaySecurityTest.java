package com.orderplatform.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityTest {

    @Autowired
    WebTestClient client;

    @Test
    void apiCallsWithoutTokenAreRejectedAtTheEdge() {
        client.get().uri("/api/orders").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/inventory").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void invalidTokenIsRejected() {
        client.get().uri("/api/orders").header("Authorization", "Bearer not-a-real-token")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void healthEndpointIsPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    @Test
    void unknownPathsAreDenied() {
        client.get().uri("/admin").exchange().expectStatus().is4xxClientError();
    }
}
