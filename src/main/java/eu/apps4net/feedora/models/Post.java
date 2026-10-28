package eu.apps4net.feedora.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

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
    
    // Image for the post's card, found in the feed entry when parsed (see ThumbnailExtractor). Null if none.
    @Column(length = 2048)
    private String thumbnail;

    // Tweets: preview of the first link in the text (page URL after redirects, title and image), found when parsed.
    // Null if the tweet has no link. A page that couldn't be read has no title, and the tweet's photo as its image (see LinkPreviewExtractor).
    @Column(length = 2048)
    private String linkUrl;
    @Column(length = 1000)
    private String linkTitle;
    @Column(length = 2048)
    private String linkImage;

    // Tweets: MP4 of the tweet's video or GIF (feeds give its thumbnail at best), found when parsed or first opened, along
    // with photos the feed didn't give. Empty if Twitter said the tweet has none, null if it wasn't checked (see TweetMediaExtractor).
    @Column(length = 2048)
    private String video;

    private LocalDateTime pubDate;
    private String author;

    @Column(name = "is_read")
    private boolean read;

    // When the post was marked as read. Read posts are deleted some time after this (see PostService.deleteExpiredReadPosts)
    private LocalDateTime readAt;

    // When the post was added, so marking all as read can skip the posts added after the list was loaded.
    // Null for posts added before it was kept.
    @CreationTimestamp
    @JsonIgnore
    private LocalDateTime createdAt;

    // Saved for later. The default lets the column be added to existing databases.
    @Column(name = "is_saved", nullable = false)
    @ColumnDefault("false")
    private boolean saved;

    // Favorite (starred). Like saved posts, favorites are never deleted by the cleanups. The default lets the column be added to existing databases.
    @Column(name = "is_favorite", nullable = false)
    @ColumnDefault("false")
    private boolean favorite;

    // YouTube videos: scheduled live stream/premiere or live now (null otherwise), and its (scheduled) start
    @Enumerated(EnumType.STRING)
    private LiveStatus liveStatus;
    private LocalDateTime liveStartAt;
    // YouTube videos: length in seconds, null if unknown (e.g. scheduled or live streams)
    private Integer duration;
    // When the live state was last checked; null for posts never checked (e.g. added before live states existed)
    @JsonIgnore
    private LocalDateTime liveCheckedAt;

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

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public String getLinkTitle() { return linkTitle; }
    public void setLinkTitle(String linkTitle) { this.linkTitle = linkTitle; }

    public String getLinkImage() { return linkImage; }
    public void setLinkImage(String linkImage) { this.linkImage = linkImage; }

    public String getVideo() { return video; }
    public void setVideo(String video) { this.video = video; }

    public LocalDateTime getPubDate() { return pubDate; }
    public void setPubDate(LocalDateTime pubDate) { this.pubDate = pubDate; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public LiveStatus getLiveStatus() { return liveStatus; }
    public void setLiveStatus(LiveStatus liveStatus) { this.liveStatus = liveStatus; }

    public LocalDateTime getLiveStartAt() { return liveStartAt; }
    public void setLiveStartAt(LocalDateTime liveStartAt) { this.liveStartAt = liveStartAt; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public LocalDateTime getLiveCheckedAt() { return liveCheckedAt; }
    public void setLiveCheckedAt(LocalDateTime liveCheckedAt) { this.liveCheckedAt = liveCheckedAt; }

    public Feed getFeed() { return feed; }
    public void setFeed(Feed feed) { this.feed = feed; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
