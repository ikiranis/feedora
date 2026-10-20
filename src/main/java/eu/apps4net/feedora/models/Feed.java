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
    // Background color shown behind the favicon (#rrggbb), e.g. when the favicon blends with the theme; null for none
    @Column(length = 7)
    private String iconBackgroundColor;

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

    public String getIconBackgroundColor() { return iconBackgroundColor; }
    public void setIconBackgroundColor(String iconBackgroundColor) { this.iconBackgroundColor = iconBackgroundColor; }

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