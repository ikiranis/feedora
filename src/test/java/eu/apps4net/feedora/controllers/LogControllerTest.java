package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.LogEntry;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogControllerTest extends ApiTestBase {
    private final UUID feedA = UUID.randomUUID();
    private final UUID feedB = UUID.randomUUID();
    private final UUID postA = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    private LogEntry oldest;
    private LogEntry middle;
    private LogEntry newest;

    private void createLogs() {
        oldest = createLog(user, feedA, null, LogLevel.ERROR, now.minusHours(3));
        middle = createLog(user, feedA, postA, LogLevel.WARNING, now.minusHours(2));
        newest = createLog(user, feedB, null, LogLevel.ERROR, now.minusHours(1));
        User other = createUser("other", "other@example.com", "secret123");
        createLog(other, feedA, null, LogLevel.ERROR, now);
    }

    @Test
    void returnsTheUsersLogsNewestFirst() throws Exception {
        createLogs();

        mockMvc.perform(auth(get("/api/logs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(newest.getId().toString(), middle.getId().toString(), oldest.getId().toString())))
                .andExpect(jsonPath("$[0].user").doesNotExist());
    }

    @Test
    void paginatesLogs() throws Exception {
        createLogs();

        mockMvc.perform(auth(get("/api/logs").param("page", "2").param("pageSize", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(oldest.getId().toString())));
    }

    @Test
    void filtersLogsByFeedPostAndLevel() throws Exception {
        createLogs();

        mockMvc.perform(auth(get("/api/logs").param("feedId", feedA.toString())))
                .andExpect(jsonPath("$[*].id", contains(middle.getId().toString(), oldest.getId().toString())));

        mockMvc.perform(auth(get("/api/logs").param("postId", postA.toString())))
                .andExpect(jsonPath("$[*].id", contains(middle.getId().toString())));

        mockMvc.perform(auth(get("/api/logs").param("level", "ERROR")))
                .andExpect(jsonPath("$[*].id", contains(newest.getId().toString(), oldest.getId().toString())));

        mockMvc.perform(auth(get("/api/logs").param("feedId", feedA.toString()).param("level", "ERROR")))
                .andExpect(jsonPath("$[*].id", contains(oldest.getId().toString())));
    }

    @Test
    void invalidFilterIsBadRequest() throws Exception {
        mockMvc.perform(auth(get("/api/logs").param("level", "FATAL")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(auth(get("/api/logs").param("feedId", "not-a-uuid")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesThePostsLogs() throws Exception {
        createLogs();

        mockMvc.perform(auth(delete("/api/logs").param("postId", postA.toString()).param("feedId", feedA.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        assertThat(logEntryRepository.findById(middle.getId())).isEmpty();
        assertThat(logEntryRepository.findById(oldest.getId())).isPresent();
    }

    @Test
    void deletesTheFeedsLogs() throws Exception {
        createLogs();

        mockMvc.perform(auth(delete("/api/logs").param("feedId", feedA.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        assertThat(logEntryRepository.findAll()).hasSize(2);
    }

    @Test
    void deletesAllTheUsersLogsOnly() throws Exception {
        createLogs();

        mockMvc.perform(auth(delete("/api/logs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        mockMvc.perform(auth(get("/api/logs"))).andExpect(jsonPath("$", hasSize(0)));
        assertThat(logEntryRepository.findAll()).hasSize(1);
    }
}
