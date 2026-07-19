package com.dumy.controller;

import com.dumy.session.SessionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void b2cRegisterLoginLogoutFlow() throws Exception {
        String username = "b2c-" + UUID.randomUUID();

        mockMvc.perform(post("/api/auth/register/b2c")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "password123"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").doesNotExist())
                .andReturn();

        String token = extractToken(loginResult);

        mockMvc.perform(post("/api/auth/logout").header("X-Session-Token", token))
                .andExpect(status().isOk());

        assertThat(SessionContext.isAuthenticated())
                .as("SessionContext must be cleared after the request completes")
                .isFalse();

        mockMvc.perform(post("/api/auth/logout").header("X-Session-Token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(3004));
    }

    @Test
    void b2bRegisterAndDomainLoginFlow() throws Exception {
        String username = "b2b-" + UUID.randomUUID();
        String domain = "tenant-" + UUID.randomUUID() + ".io";

        mockMvc.perform(post("/api/auth/register/b2b")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantName", "Acme",
                                "domain", domain,
                                "username", username,
                                "password", "password123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "password123",
                                "domain", domain))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionToken").exists())
                .andExpect(jsonPath("$.data.tenantId").exists());
    }

    @Test
    void b2bUserOmittingDomainIsRejectedAsInvalidCredentials() throws Exception {
        String username = "b2b-" + UUID.randomUUID();
        String domain = "tenant-" + UUID.randomUUID() + ".io";

        mockMvc.perform(post("/api/auth/register/b2b")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantName", "Acme",
                                "domain", domain,
                                "username", username,
                                "password", "password123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(3001));
    }

    private String extractToken(MvcResult result) throws Exception {
        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        return (String) data.get("sessionToken");
    }
}
