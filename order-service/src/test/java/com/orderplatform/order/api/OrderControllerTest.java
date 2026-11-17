package com.orderplatform.order.api;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.orderplatform.order.config.SecurityConfig;
import com.orderplatform.order.domain.Order;
import com.orderplatform.order.domain.OrderItem;
import com.orderplatform.order.domain.OrderNotFoundException;
import com.orderplatform.order.domain.OrderService;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    private static final String VALID_BODY = """
            {"lines":[{"sku":"KEYBOARD-01","quantity":2}]}
            """;

    @Autowired
    MockMvc mvc;

    @MockBean
    OrderService orderService;

    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }

    @Test
    void rejectsUsersWithoutCustomerRole() throws Exception {
        mvc.perform(post("/api/orders").with(userWithRoles("alice")).contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderService);
    }

    @Test
    void customerCanPlaceOrder() throws Exception {
        Order order = new Order("alice", List.of(new OrderItem("KEYBOARD-01", 2)));
        when(orderService.placeOrder(eq("alice"), anyList())).thenReturn(order);

        mvc.perform(post("/api/orders").with(userWithRoles("alice", "ROLE_CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/orders/" + order.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerId").value("alice"));
    }

    @Test
    void validatesRequestBody() throws Exception {
        mvc.perform(post("/api/orders").with(userWithRoles("alice", "ROLE_CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"lines\":[{\"sku\":\"\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    void customerCannotReadSomeoneElsesOrder() throws Exception {
        Order bobsOrder = new Order("bob", List.of(new OrderItem("MOUSE-01", 1)));
        when(orderService.getOrder(bobsOrder.getId())).thenReturn(bobsOrder);

        mvc.perform(get("/api/orders/" + bobsOrder.getId()).with(userWithRoles("alice", "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyOrder() throws Exception {
        Order bobsOrder = new Order("bob", List.of(new OrderItem("MOUSE-01", 1)));
        when(orderService.getOrder(bobsOrder.getId())).thenReturn(bobsOrder);

        mvc.perform(get("/api/orders/" + bobsOrder.getId()).with(userWithRoles("admin", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("bob"));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(id)).thenThrow(new OrderNotFoundException(id));

        mvc.perform(get("/api/orders/" + id).with(userWithRoles("alice", "ROLE_CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    /** jwt() bypasses our JwtAuthenticationConverter and names the principal after "sub", so set both. */
    private static RequestPostProcessor userWithRoles(String username, String... roles) {
        return jwt().jwt(j -> j.subject(username).claim("preferred_username", username))
                .authorities(Arrays.stream(roles).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
    }
}
