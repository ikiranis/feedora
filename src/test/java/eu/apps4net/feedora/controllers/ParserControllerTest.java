package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.Parse;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.ParseService;
import eu.apps4net.feedora.support.ApiTestBase;
import eu.apps4net.feedora.support.TestHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParserControllerTest extends ApiTestBase {
    private static final String NEWS_PAGE = "<html><head><title>Local News</title></head><body><main>"
            + "<div class=\"story\"><h3><a href=\"/s/1\">The first story of the page</a></h3></div>"
            + "<div class=\"story\"><h3><a href=\"/s/2\">The second story of the page</a></h3></div>"
            + "<div class=\"story\"><h3><a href=\"/s/3\">The third story of the page</a></h3></div>"
            + "</main></body></html>";

    @Autowired
    private ParseService parseService;

    private TestHttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new TestHttpServer()
                .respond("/news", 200, "text/html", NEWS_PAGE)
                .respond("/blocked", 503, "text/html", "<html><head><title>Just a moment...</title></head></html>");
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    private String body(String url, String selector, Integer frequency) {
        return "{\"url\":\"" + url + "\",\"title\":\"Local News\",\"selector\":\"" + selector + "\",\"frequency\":" + frequency + "}";
    }

    private Parse addNewsPage() throws Exception {
        mockMvc.perform(auth(post("/api/parser/pages")).contentType(MediaType.APPLICATION_JSON)
                        .content(body(server.url("/news"), "div.story", null)))
                .andExpect(status().isOk());
        return parseRepository.findAllByUser(user).get(0);
    }

    @Test
    void analyzeReturnsTheRepeatedElements() throws Exception {
        mockMvc.perform(auth(post("/api/parser/analyze")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + server.url("/news") + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Local News"))
                .andExpect(jsonPath("$.candidates[0].selector").value("div.story"))
                .andExpect(jsonPath("$.candidates[0].count").value(3));
    }

    @Test
    void analyzeReportsBlockedPages() throws Exception {
        mockMvc.perform(auth(post("/api/parser/analyze")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + server.url("/blocked") + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blocked").value(true));
    }

    @Test
    void addingAPageParsesItIntoAPublicFeed() throws Exception {
        mockMvc.perform(auth(post("/api/parser/pages")).contentType(MediaType.APPLICATION_JSON)
                        .content(body(server.url("/news"), "div.story", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value(Parse.DEFAULT_FREQUENCY_MINUTES))
                .andExpect(jsonPath("$.feedName").value("127.0.0.1"))
                .andExpect(jsonPath("$.lastItemCount").value(3))
                .andExpect(jsonPath("$.lastError").doesNotExist())
                .andExpect(jsonPath("$.inFeeds").value(false));

        // No token needed
        mockMvc.perform(get("/api/parser/feed/127.0.0.1.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rss+xml"))
                .andExpect(content().string(containsString("The second story of the page")));

        mockMvc.perform(auth(get("/api/parser/pages")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Local News"))
                .andExpect(jsonPath("$[0].user").doesNotExist());
    }

    @Test
    void rejectsDuplicatePagesAndInvalidValues() throws Exception {
        addNewsPage();

        mockMvc.perform(auth(post("/api/parser/pages")).contentType(MediaType.APPLICATION_JSON)
                        .content(body(server.url("/news"), "div.story", null)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("This page is already parsed"));

        mockMvc.perform(auth(post("/api/parser/pages")).contentType(MediaType.APPLICATION_JSON)
                        .content(body(server.url("/other"), "div.story", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("The frequency must be between 5 and 10080 minutes"));
    }

    @Test
    void updatesAndAddsThePageToTheFeeds() throws Exception {
        Parse parse = addNewsPage();

        mockMvc.perform(auth(put("/api/parser/pages/" + parse.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "div.story", 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value(30));

        Folder folder = createFolder("Local", user);
        mockMvc.perform(auth(post("/api/parser/pages/" + parse.getId() + "/addFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folderId\":\"" + folder.getId() + "\"}"))
                .andExpect(status().isOk());

        assertThat(feedRepository.findByUser(user)).singleElement().satisfies(feed -> {
            assertThat(feed.getXmlUrl()).endsWith("/api/parser/feed/127.0.0.1.xml");
            assertThat(feed.getFolder().getId()).isEqualTo(folder.getId());
        });
        mockMvc.perform(auth(get("/api/parser/pages")))
                .andExpect(jsonPath("$[0].inFeeds").value(true));
    }

    @Test
    void deletingAPageRemovesItsFeedFile() throws Exception {
        Parse parse = addNewsPage();

        mockMvc.perform(auth(delete("/api/parser/pages/" + parse.getId())))
                .andExpect(status().isOk());

        assertThat(parseRepository.findAll()).isEmpty();
        mockMvc.perform(get("/api/parser/feed/127.0.0.1.xml")).andExpect(status().isNotFound());
    }

    @Test
    void otherUsersCantChangeThePage() throws Exception {
        Parse parse = addNewsPage();
        User other = createUser("other", "other@example.com", "secret123");

        mockMvc.perform(authAs(other, delete("/api/parser/pages/" + parse.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Page not found"));
        assertThat(parseRepository.findAll()).hasSize(1);
    }

    @Test
    void feedRouteRejectsOtherFiles() throws Exception {
        // Rejected by the firewall before reaching the route
        mockMvc.perform(get("/api/parser/feed/..%2Fpom.xml")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/parser/feed/pom")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/parser/feed/missing.xml")).andExpect(status().isNotFound());
    }

    @Test
    void generatedFeedsMoveToTheCurrentPort() {
        Folder folder = createFolder("Local", user);
        Parse parse = new Parse();
        parse.setFeedName("example.com");
        createFeed("Old port", "http://localhost:9999/api/parser/feed/example.com.xml", folder, user);
        createFeed("Other site", "https://example.com/api/parser/feed/example.com.xml", folder, user);

        parseService.migrateFeedUrls();

        assertThat(feedRepository.findByUser(user)).extracting(feed -> feed.getXmlUrl()).containsExactlyInAnyOrder(
                parseService.feedUrl(parse),
                "https://example.com/api/parser/feed/example.com.xml");
    }
}
