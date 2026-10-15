package eu.apps4net.feedora.models;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModelsTest {

    @Test
    void feedExposesItsFolderNameAndId() {
        Folder folder = new Folder("News", null);
        folder.setId(UUID.randomUUID());
        Feed feed = new Feed("Feed", "http://a/rss", "http://a", "rss", folder, null);

        assertThat(feed.getFolderName()).isEqualTo("News");
        assertThat(feed.getFolderId()).isEqualTo(folder.getId());

        feed.setFolder(null);
        assertThat(feed.getFolderName()).isNull();
        assertThat(feed.getFolderId()).isNull();
    }

    @Test
    void feedDtoCopiesTheFeed() {
        Feed feed = new Feed("Feed", "http://a/rss", "http://a", "rss", new Folder("News", null), null);
        feed.setId(UUID.randomUUID());

        FeedDTO dto = FeedDTO.fromFeed(feed);

        assertThat(dto.id).isEqualTo(feed.getId().toString());
        assertThat(dto.title).isEqualTo("Feed");
        assertThat(dto.xmlUrl).isEqualTo("http://a/rss");
        assertThat(dto.htmlUrl).isEqualTo("http://a");
        assertThat(dto.type).isEqualTo("rss");
        assertThat(dto.folderName).isEqualTo("News");
        assertThat(dto.logCount).isZero();
    }

    @Test
    void feedDtoWithoutFolder() {
        Feed feed = new Feed("Feed", "http://a/rss", "http://a", "rss", null, null);
        feed.setId(UUID.randomUUID());

        assertThat(FeedDTO.fromFeed(feed).folderName).isNull();
    }

    @Test
    void languageDtoToString() {
        assertThat(new LanguageDTO("Hello", "Γεια").toString()).isEqualTo("LanguageDTO{text='Hello', translation='Γεια'}");
    }

    @Test
    void requestAndResponseObjectsKeepTheirValues() {
        UUID id = UUID.randomUUID();
        AuthResponse response = new AuthResponse("token", "a@b.com", "name", id);
        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getEmail()).isEqualTo("a@b.com");
        assertThat(response.getUsername()).isEqualTo("name");
        assertThat(response.getUserId()).isEqualTo(id);

        LoginRequest login = new LoginRequest("a@b.com", "pass");
        assertThat(login.getEmail()).isEqualTo("a@b.com");
        assertThat(login.getPassword()).isEqualTo("pass");

        RegisterRequest register = new RegisterRequest("name", "a@b.com", "pass");
        assertThat(register.getUsername()).isEqualTo("name");
        assertThat(register.getEmail()).isEqualTo("a@b.com");
        assertThat(register.getPassword()).isEqualTo("pass");
    }
}
