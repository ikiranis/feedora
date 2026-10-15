package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.Role;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends ApiTestBase {

    private static final String USER_JSON = "{\"username\":\"renamed\",\"email\":\"%s\",\"password\":\"\"}";

    @Test
    void adminSeesAllUsers() throws Exception {
        createUser("second", "second@example.com", "secret123");

        mockMvc.perform(auth(get("/api/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", containsInAnyOrder("tester@example.com", "second@example.com")))
                .andExpect(jsonPath("$[?(@.email == 'tester@example.com')].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].folders").doesNotExist())
                .andExpect(jsonPath("$[0].feeds").doesNotExist());
    }

    @Test
    void simpleUserSeesOnlyThemselves() throws Exception {
        User simple = createUser("second", "second@example.com", "secret123");

        mockMvc.perform(authAs(simple, get("/api/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("second@example.com"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    void simpleUserCanUpdateOnlyThemselves() throws Exception {
        User simple = createUser("second", "second@example.com", "secret123");

        mockMvc.perform(authAs(simple, put("/api/users/" + simple.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(USER_JSON.formatted("second@example.com")))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(simple.getId()).orElseThrow().getUsername()).isEqualTo("renamed");

        mockMvc.perform(authAs(simple, put("/api/users/" + user.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(USER_JSON.formatted("tester@example.com")))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.findById(user.getId()).orElseThrow().getUsername()).isEqualTo("tester");
    }

    @Test
    void simpleUserCantManageUsers() throws Exception {
        User simple = createUser("second", "second@example.com", "secret123");
        User third = createUser("third", "third@example.com", "secret123");

        mockMvc.perform(authAs(simple, post("/api/users")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"email\":\"x@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(authAs(simple, delete("/api/users/" + third.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(authAs(simple, put("/api/users/" + simple.getId() + "/role"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(simple.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);
        assertThat(userRepository.findById(third.getId())).isPresent();
    }

    @Test
    void adminChangesTheRoleOfOtherUsers() throws Exception {
        User simple = createUser("second", "second@example.com", "secret123");

        mockMvc.perform(auth(put("/api/users/" + simple.getId() + "/role"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(simple.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);

        mockMvc.perform(auth(put("/api/users/" + simple.getId() + "/role"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"NOPE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid role"));
    }

    @Test
    void adminCantChangeTheirOwnRole() throws Exception {
        mockMvc.perform(auth(put("/api/users/" + user.getId() + "/role"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You can't change your own role"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
}
