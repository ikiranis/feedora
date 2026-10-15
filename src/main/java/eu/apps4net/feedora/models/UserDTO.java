package eu.apps4net.feedora.models;

public class UserDTO {
    public String id;
    public String username;
    public String email;
    public long folderCount;
    public long feedCount;
    public long postCount;
    public long unreadCount;
    public long savedCount;
    public long logCount;

    public static UserDTO fromUser(User user) {
        UserDTO dto = new UserDTO();
        dto.id = user.getId().toString();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        return dto;
    }
}
