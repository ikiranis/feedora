package eu.apps4net.feedora.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A web page without a feed, whose posts are read from its repeated elements and saved as an RSS file
 * in the feeds folder.
 */
@Entity
@Table(name = "parse_page")
public class Parse {
    // How often a page is parsed, when no frequency is given
    public static final int DEFAULT_FREQUENCY_MINUTES = 60;
    public static final int MIN_FREQUENCY_MINUTES = 5;
    // A week
    public static final int MAX_FREQUENCY_MINUTES = 10080;

    @Id
    @GeneratedValue
    private UUID id;

    @Column(length = 2048)
    private String url;

    private String title;

    // CSS selector of the repeated elements that hold the posts
    @Column(length = 1024)
    private String selector;

    // Minutes between two parses
    private int frequency = DEFAULT_FREQUENCY_MINUTES;

    // Name of the RSS file in the feeds folder, without the .xml extension (the page's domain)
    @Column(unique = true)
    private String feedName;

    private LocalDateTime createdAt;

    // Null until the page is parsed for the first time
    private LocalDateTime lastParsedAt;

    // Posts found by the last parse
    private int lastItemCount;

    // Error of the last parse, null when it succeeded
    @Column(length = 2048)
    private String lastError;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    public Parse() {}

    public Parse(String url, String title, String selector, int frequency, String feedName, User user) {
        this.url = url;
        this.title = title;
        this.selector = selector;
        this.frequency = frequency;
        this.feedName = feedName;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Whether the page hasn't been parsed yet, or its frequency has passed since the last parse
     */
    public boolean isDue(LocalDateTime now) {
        return lastParsedAt == null || !lastParsedAt.plusMinutes(frequency).isAfter(now);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSelector() { return selector; }
    public void setSelector(String selector) { this.selector = selector; }

    public int getFrequency() { return frequency; }
    public void setFrequency(int frequency) { this.frequency = frequency; }

    public String getFeedName() { return feedName; }
    public void setFeedName(String feedName) { this.feedName = feedName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastParsedAt() { return lastParsedAt; }
    public void setLastParsedAt(LocalDateTime lastParsedAt) { this.lastParsedAt = lastParsedAt; }

    public int getLastItemCount() { return lastItemCount; }
    public void setLastItemCount(int lastItemCount) { this.lastItemCount = lastItemCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
