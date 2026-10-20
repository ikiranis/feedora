package eu.apps4net.feedora.models;

public class FeedDTO {
    public String id;
    public String title;
    public String xmlUrl;
    public String htmlUrl;
    public String type;
    public String folderName;
    // Color of the feed's cards chosen by the user (#rrggbb), null for none
    public String cardColor;
    // Main color of the favicon (#rrggbb): null until checked, empty when it has no clear color
    public String faviconColor;
    // Number of log entries of the feed itself, not counting those of its posts
    public long logCount;
    // Null until the feed is checked for the first time
    public FeedStatistics statistics;

    public static FeedDTO fromFeed(Feed feed) {
        FeedDTO dto = new FeedDTO();
        dto.id = feed.getId().toString();
        dto.title = feed.getTitle();
        dto.xmlUrl = feed.getXmlUrl();
        dto.htmlUrl = feed.getHtmlUrl();
        dto.type = feed.getType();
        dto.cardColor = feed.getCardColor();
        dto.faviconColor = feed.getFaviconColor();
        dto.folderName = feed.getFolder() != null ? feed.getFolder().getName() : null;
        dto.statistics = feed.getStatistics();
        return dto;
    }
}
