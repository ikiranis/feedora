package eu.apps4net.feedora.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({"user"}) // Only ignore user, not feed
public class Post {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(length = 1000)
    private String title;
    private String link;
    
    @Lob
    private String description;
    
    private LocalDateTime pubDate;
    private String author;

    @Column(name = "is_read")
    private boolean read;

    @ManyToOne
    @JoinColumn(name = "feed_id")
    private Feed feed;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Post() {}

    public Post(String title, String link, String description, LocalDateTime pubDate, String author, boolean read, Feed feed, User user) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.pubDate = pubDate;
        this.author = author;
        this.read = read;
        this.feed = feed;
        this.user = user;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getPubDate() { return pubDate; }
    public void setPubDate(LocalDateTime pubDate) { this.pubDate = pubDate; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Feed getFeed() { return feed; }
    public void setFeed(Feed feed) { this.feed = feed; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
