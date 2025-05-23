package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.services.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderController {
    @Autowired
    private FolderService folderService;

    @GetMapping
    public List<Folder> getAllFolders() {
        return folderService.getAllFolders();
    }
}
