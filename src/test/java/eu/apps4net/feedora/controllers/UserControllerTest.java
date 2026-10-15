package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends ApiTestBase {

    @Test
    void returnsAllUsers() throws Exception {
        createUser("second", "second@example.com", "secret123");

        mockMvc.perform(auth(get("/api/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", containsInAnyOrder("tester@example.com", "second@example.com")))
                .andExpect(jsonPath("$[0].folders").doesNotExist())
                .andExpect(jsonPath("$[0].feeds").doesNotExist());
    }
}
