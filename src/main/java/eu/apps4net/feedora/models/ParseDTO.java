package eu.apps4net.feedora.models;

import java.time.LocalDateTime;

public class ParseDTO {
    public String id;
    public String url;
    public String title;
    public String selector;
    public int frequency;
    public String feedName;
    // Path of the generated feed on this server, e.g. /api/parser/feed/example.com.xml
    public String feedPath;
    public LocalDateTime createdAt;
    public LocalDateTime lastParsedAt;
    public int lastItemCount;
    public String lastError;
    // The generated feed is already in the user's feeds
    public boolean inFeeds;

    public static ParseDTO fromParse(Parse parse) {
        ParseDTO dto = new ParseDTO();
        dto.id = parse.getId().toString();
        dto.url = parse.getUrl();
        dto.title = parse.getTitle();
        dto.selector = parse.getSelector();
        dto.frequency = parse.getFrequency();
        dto.feedName = parse.getFeedName();
        dto.feedPath = "/api/parser/feed/" + parse.getFeedName() + ".xml";
        dto.createdAt = parse.getCreatedAt();
        dto.lastParsedAt = parse.getLastParsedAt();
        dto.lastItemCount = parse.getLastItemCount();
        dto.lastError = parse.getLastError();
        return dto;
    }
}
