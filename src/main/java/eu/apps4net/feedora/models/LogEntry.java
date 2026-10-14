package eu.apps4net.feedora.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A log entry of something that went wrong while processing a feed or a post.
 * The feed and post are kept as plain ids (with a snapshot of their title), not as relations,
 * so the entries survive when the feed or post is deleted and can still be retrieved by its id.
 */
@Entity
@Table(name = "log_entry", indexes = {
        @Index(name = "idx_log_entry_feed", columnList = "feedId"),
        @Index(name = "idx_log_entry_post", columnList = "postId"),
        @Index(name = "idx_log_entry_created", columnList = "createdAt")
})
public class LogEntry {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LogType type;

    @Column(length = 2000)
    private String message;

    // Extra information for debugging, e.g. the exception class or a snippet of the invalid XML
    @Lob
    private String details;

    private UUID feedId;
    @Column(length = 1000)
    private String feedTitle;

    private UUID postId;
    @Column(length = 1000)
    private String postTitle;

    // The URL that was being processed (feed URL or post link)
    @Column(length = 2048)
    private String url;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    public LogEntry() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LogLevel getLevel() { return level; }
    public void setLevel(LogLevel level) { this.level = level; }

    public LogType getType() { return type; }
    public void setType(LogType type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public UUID getFeedId() { return feedId; }
    public void setFeedId(UUID feedId) { this.feedId = feedId; }

    public String getFeedTitle() { return feedTitle; }
    public void setFeedTitle(String feedTitle) { this.feedTitle = feedTitle; }

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
