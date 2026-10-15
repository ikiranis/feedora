package eu.apps4net.feedora.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.ColumnDefault;

@Entity
@JsonIgnoreProperties({"user"}) // Only ignore user, not feed
public class Post {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(length = 1000)
    private String title;
    
    @Column(length = 2048)
    private String link;
    
    @Lob
    private String description;

    // Full sanitized article HTML, from the feed or extracted from the page. Served by its own endpoint.
    @Lob
    @JsonIgnore
    private String content;
    
    private LocalDateTime pubDate;
    private String author;

    @Column(name = "is_read")
    private boolean read;

    // When the post was marked as read. Read posts are deleted some time after this (see PostService.deleteExpiredReadPosts)
    private LocalDateTime readAt;

    // Saved for later. The default lets the column be added to existing databases.
    @Column(name = "is_saved", nullable = false)
    @ColumnDefault("false")
    private boolean saved;

    @ManyToOne
    @JoinColumn(name = "feed_id")
    private Feed feed;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Post() {}

    public Post(String title, String link, String description, LocalDateTime pubDate, String author, boolean read) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.pubDate = pubDate;
        this.author = author;
        this.read = read;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getPubDate() { return pubDate; }
    public void setPubDate(LocalDateTime pubDate) { this.pubDate = pubDate; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }

    public Feed getFeed() { return feed; }
    public void setFeed(Feed feed) { this.feed = feed; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
