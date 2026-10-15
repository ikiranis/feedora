package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends ApiTestBase {

    private String json(String username, String email, String password) {
        StringBuilder body = new StringBuilder("{");
        if (username != null) body.append("\"username\":\"").append(username).append("\",");
        if (email != null) body.append("\"email\":\"").append(email).append("\",");
        if (password != null) body.append("\"password\":\"").append(password).append("\",");
        if (body.length() > 1) body.setLength(body.length() - 1);
        return body.append("}").toString();
    }

    @Test
    void registerCreatesUserAndReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("  newbie ", " NewBie@Example.com ", "password1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.email").value("newbie@example.com"))
                .andExpect(jsonPath("$.username").value("newbie"))
                .andExpect(jsonPath("$.userId").isNotEmpty());

        User saved = userRepository.findByEmail("newbie@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("password1");
        assertThat(passwordEncoder.matches("password1", saved.getPassword())).isTrue();
    }

    @Test
    void registerValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(" ", "a@b.com", "password1")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username is required"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", null, "password1")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email is required"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "a@b.com", "12345")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Password must be at least 6 characters long"));
    }

    @Test
    void registerRejectsExistingEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("other", "TESTER@example.com", "password1")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User with email tester@example.com already exists"));
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, " Tester@Example.com ", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tester@example.com"))
                .andExpect(jsonPath("$.username").value("tester"))
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andReturn().getResponse().getContentAsString();

        String issuedToken = com.jayway.jsonpath.JsonPath.read(response, "$.token");
        assertThat(jwtUtil.extractEmail(issuedToken)).isEqualTo("tester@example.com");
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "tester@example.com", "wrong-password")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "nobody@example.com", "secret123")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void loginValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "", "secret123")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email is required"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "tester@example.com", "")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Password is required"));
    }

    @Test
    void meReturnsTheAuthenticatedUserWithoutToken() throws Exception {
        mockMvc.perform(auth(get("/api/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tester@example.com"))
                .andExpect(jsonPath("$.username").value("tester"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void meWithoutAuthenticationIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Please check your input"));
    }

    @Test
    void logoutSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));
    }
}
