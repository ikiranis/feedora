package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GeneralControllerTest extends ApiTestBase {

    @Test
    void appAliveReturnsTrue() throws Exception {
        mockMvc.perform(get("/api/general/appAlive"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
