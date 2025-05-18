package eu.apps4net.feedora.models;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
public class Folder {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @OneToMany(mappedBy = "folder")
    private List<Feed> feeds;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Folder() {}

    public Folder(String name, User user) {
        this.name = name;
        this.user = user;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Feed> getFeeds() { return feeds; }
    public void setFeeds(List<Feed> feeds) { this.feeds = feeds; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
