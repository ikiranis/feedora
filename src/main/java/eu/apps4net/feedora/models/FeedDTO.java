package eu.apps4net.feedora.models;

public class FeedDTO {
    public String id;
    public String title;
    public String xmlUrl;
    public String htmlUrl;
    public String type;
    public String folderName;

    public static FeedDTO fromFeed(Feed feed) {
        FeedDTO dto = new FeedDTO();
        dto.id = feed.getId().toString();
        dto.title = feed.getTitle();
        dto.xmlUrl = feed.getXmlUrl();
        dto.htmlUrl = feed.getHtmlUrl();
        dto.type = feed.getType();
        dto.folderName = feed.getFolder() != null ? feed.getFolder().getName() : null;
        return dto;
    }
}
