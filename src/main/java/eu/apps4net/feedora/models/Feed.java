package eu.apps4net.feedora.models;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({"posts"})
public class Feed {
    @Id
    @GeneratedValue
    private UUID id;

    private String title;
    @Column(length = 1000)
    private String xmlUrl;
    @Column(length = 1000)
    private String htmlUrl;
    private String type;
    // Color chosen by the user for the light background of the feed's cards (#rrggbb), replacing faviconColor; null for none
    @Column(length = 7)
    private String cardColor;
    // Main color of the favicon of the feed's website, or of the channel's avatar for YouTube channels (#rrggbb):
    // null until it's checked, empty when it has no clear color.
    // Column renamed from favicon_color, so the colors found with youtube.com's favicon for every channel are checked again.
    @Column(name = "icon_color", length = 7)
    private String faviconColor;

    @ManyToOne
    @JoinColumn(name = "folder_id")
    @JsonIgnore
    private Folder folder;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "feed")
    @JsonIgnore
    private List<Post> posts;

    // Removed together with the feed
    @OneToOne(mappedBy = "feed", cascade = CascadeType.REMOVE)
    private FeedStatistics statistics;

    public Feed() {}

    public Feed(String title, String xmlUrl, String htmlUrl, String type, Folder folder, User user) {
        this.title = title;
        this.xmlUrl = xmlUrl;
        this.htmlUrl = htmlUrl;
        this.type = type;
        this.folder = folder;
        this.user = user;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getXmlUrl() { return xmlUrl; }
    public void setXmlUrl(String xmlUrl) { this.xmlUrl = xmlUrl; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCardColor() { return cardColor; }
    public void setCardColor(String cardColor) { this.cardColor = cardColor; }

    public String getFaviconColor() { return faviconColor; }
    public void setFaviconColor(String faviconColor) { this.faviconColor = faviconColor; }

    public Folder getFolder() { return folder; }
    public void setFolder(Folder folder) { this.folder = folder; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Post> getPosts() { return posts; }
    public void setPosts(List<Post> posts) { this.posts = posts; }

    public FeedStatistics getStatistics() { return statistics; }
    public void setStatistics(FeedStatistics statistics) { this.statistics = statistics; }

    // Expose folderName for serialization
    public String getFolderName() {
        return folder != null ? folder.getName() : null;
    }

    // Expose folderId for serialization
    public UUID getFolderId() {
        return folder != null ? folder.getId() : null;
    }
}