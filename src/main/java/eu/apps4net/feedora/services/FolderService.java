package eu.apps4net.feedora.services;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FolderService {
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private FeedRepository feedRepository;

    public List<Folder> getAllFolders() {
        return folderRepository.findAll();
    }

    public List<Folder> getAllFoldersByUser(User user) {
        return folderRepository.findAllByUser(user);
    }
    
    @Transactional
    public String addFolder(String name, User user) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(Language.getString("Folder name cannot be empty"));
        }
        
        String trimmedName = name.trim();
        
        // Check if folder already exists for this user
        Optional<Folder> existingFolder = folderRepository.findByNameAndUser(trimmedName, user);
        if (existingFolder.isPresent()) {
            throw new IllegalArgumentException(Language.getString("Folder already exists"));
        }
        
        // Create new folder
        Folder folder = new Folder(trimmedName, user);
        folderRepository.save(folder);
        
        return Language.getString("Folder added successfully");
    }
    
    @Transactional
    public String deleteFolder(String folderId, User user) {
        if (folderId == null || folderId.trim().isEmpty()) {
            throw new IllegalArgumentException(Language.getString("Invalid folder ID"));
        }
        
        UUID folderUuid;
        try {
            folderUuid = UUID.fromString(folderId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(Language.getString("Invalid folder ID"));
        }
        
        Optional<Folder> folderOpt = folderRepository.findById(folderUuid);
        if (folderOpt.isEmpty()) {
            throw new IllegalArgumentException(Language.getString("Folder not found"));
        }
        
        Folder folder = folderOpt.get();
        
        // Check if folder belongs to user
        if (!folder.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(Language.getString("Folder doesn't belong to user"));
        }
        
        // Move all feeds from this folder to the default folder (null/empty folder)
        List<Feed> feedsInFolder = feedRepository.findByFolder(folder);
        for (Feed feed : feedsInFolder) {
            feed.setFolder(null);
            feedRepository.save(feed);
        }
        
        // Delete the folder
        folderRepository.delete(folder);
        
        return Language.getString("Folder deleted successfully");
    }
    
    @Transactional
    public String updateFolder(String folderId, String newName, User user) {
        if (folderId == null || folderId.trim().isEmpty()) {
            throw new IllegalArgumentException(Language.getString("Invalid folder ID"));
        }
        
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException(Language.getString("Folder name cannot be empty"));
        }
        
        String trimmedName = newName.trim();
        
        UUID folderUuid;
        try {
            folderUuid = UUID.fromString(folderId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(Language.getString("Invalid folder ID"));
        }
        
        Optional<Folder> folderOpt = folderRepository.findById(folderUuid);
        if (folderOpt.isEmpty()) {
            throw new IllegalArgumentException(Language.getString("Folder not found"));
        }
        
        Folder folder = folderOpt.get();
        
        // Check if folder belongs to user
        if (!folder.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(Language.getString("Folder doesn't belong to user"));
        }
        
        // Check if another folder with the same name already exists for this user (excluding current folder)
        Optional<Folder> existingFolder = folderRepository.findByNameAndUser(trimmedName, user);
        if (existingFolder.isPresent() && !existingFolder.get().getId().equals(folder.getId())) {
            throw new IllegalArgumentException(Language.getString("Folder already exists"));
        }
        
        // Update folder name
        folder.setName(trimmedName);
        folderRepository.save(folder);
        
        return Language.getString("Folder updated successfully");
    }
}
