package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FolderControllerTest extends ApiTestBase {

    @Test
    void returnsOnlyTheCurrentUsersFolders() throws Exception {
        createFolder("News", user);
        createFolder("Tech", user);
        createFolder("Other user's", createUser("other", "other@example.com", "secret123"));

        mockMvc.perform(auth(get("/api/folders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("News", "Tech")))
                .andExpect(jsonPath("$[0].user").doesNotExist());
    }

    @Test
    void addsAFolderWithTrimmedName() throws Exception {
        mockMvc.perform(auth(post("/api/addFolder")).contentType(MediaType.TEXT_PLAIN).content("  News  "))
                .andExpect(status().isOk())
                .andExpect(content().string("Folder added successfully"));

        assertThat(folderRepository.findByNameAndUser("News", user)).isPresent();
    }

    @Test
    void addFolderRejectsDuplicatesAndBlankNames() throws Exception {
        createFolder("News", user);

        mockMvc.perform(auth(post("/api/addFolder")).contentType(MediaType.TEXT_PLAIN).content("News"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error adding folder: Folder already exists"));

        mockMvc.perform(auth(post("/api/addFolder")).contentType(MediaType.TEXT_PLAIN).content("   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error adding folder: Folder name cannot be empty"));
    }

    @Test
    void deletingAFolderKeepsItsFeedsWithoutFolder() throws Exception {
        Folder folder = createFolder("News", user);
        Feed feed = createFeed("Feed", "http://example.com/rss", folder, user);

        mockMvc.perform(auth(delete("/api/deleteFolder/" + folder.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("Folder deleted successfully"));

        assertThat(folderRepository.findById(folder.getId())).isEmpty();
        assertThat(feedRepository.findById(feed.getId()).orElseThrow().getFolder()).isNull();
    }

    @Test
    void deleteFolderRejectsInvalidMissingAndForeignFolders() throws Exception {
        User other = createUser("other", "other@example.com", "secret123");
        Folder foreign = createFolder("Foreign", other);

        mockMvc.perform(auth(delete("/api/deleteFolder/not-a-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error deleting folder: Invalid folder ID"));

        mockMvc.perform(auth(delete("/api/deleteFolder/" + UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error deleting folder: Folder not found"));

        mockMvc.perform(auth(delete("/api/deleteFolder/" + foreign.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error deleting folder: Folder doesn't belong to user"));

        assertThat(folderRepository.findById(foreign.getId())).isPresent();
    }

    @Test
    void renamesAFolder() throws Exception {
        Folder folder = createFolder("News", user);

        mockMvc.perform(auth(put("/api/updateFolder/" + folder.getId())).contentType(MediaType.TEXT_PLAIN).content(" World "))
                .andExpect(status().isOk())
                .andExpect(content().string("Folder updated successfully"));

        assertThat(folderRepository.findById(folder.getId()).orElseThrow().getName()).isEqualTo("World");
    }

    @Test
    void renamingAFolderToItsOwnNameIsAllowed() throws Exception {
        Folder folder = createFolder("News", user);

        mockMvc.perform(auth(put("/api/updateFolder/" + folder.getId())).contentType(MediaType.TEXT_PLAIN).content("News"))
                .andExpect(status().isOk());
    }

    @Test
    void updateFolderRejectsDuplicateNamesAndForeignFolders() throws Exception {
        Folder folder = createFolder("News", user);
        createFolder("Tech", user);
        Folder foreign = createFolder("Foreign", createUser("other", "other@example.com", "secret123"));

        mockMvc.perform(auth(put("/api/updateFolder/" + folder.getId())).contentType(MediaType.TEXT_PLAIN).content("Tech"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error updating folder: Folder already exists"));

        mockMvc.perform(auth(put("/api/updateFolder/" + foreign.getId())).contentType(MediaType.TEXT_PLAIN).content("Mine"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error updating folder: Folder doesn't belong to user"));

        mockMvc.perform(auth(put("/api/updateFolder/" + UUID.randomUUID())).contentType(MediaType.TEXT_PLAIN).content("Mine"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error updating folder: Folder not found"));

        mockMvc.perform(auth(put("/api/updateFolder/" + folder.getId())).contentType(MediaType.TEXT_PLAIN).content(" "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error updating folder: Folder name cannot be empty"));
    }
}
