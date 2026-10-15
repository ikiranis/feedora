package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private FeedRepository feedRepository;
    @InjectMocks
    private FolderService folderService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("tester", "tester@example.com", "hash");
        user.setId(UUID.randomUUID());
    }

    private Folder folder(String name, User owner) {
        Folder folder = new Folder(name, owner);
        folder.setId(UUID.randomUUID());
        return folder;
    }

    @Test
    void addFolderSavesTrimmedName() {
        when(folderRepository.findByNameAndUser("News", user)).thenReturn(Optional.empty());

        assertThat(folderService.addFolder("  News ", user)).isEqualTo("Folder added successfully");

        ArgumentCaptor<Folder> saved = ArgumentCaptor.forClass(Folder.class);
        verify(folderRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("News");
        assertThat(saved.getValue().getUser()).isSameAs(user);
    }

    @Test
    void addFolderRejectsEmptyAndDuplicateNames() {
        assertThatThrownBy(() -> folderService.addFolder(null, user)).hasMessage("Folder name cannot be empty");
        assertThatThrownBy(() -> folderService.addFolder(" ", user)).hasMessage("Folder name cannot be empty");

        when(folderRepository.findByNameAndUser("News", user)).thenReturn(Optional.of(folder("News", user)));
        assertThatThrownBy(() -> folderService.addFolder("News", user)).hasMessage("Folder already exists");
        verify(folderRepository, never()).save(any());
    }

    @Test
    void deleteFolderMovesFeedsOutOfIt() {
        Folder folder = folder("News", user);
        Feed feed = new Feed("Feed", "http://a/rss", "", "rss", folder, user);
        when(folderRepository.findById(folder.getId())).thenReturn(Optional.of(folder));
        when(feedRepository.findByFolder(folder)).thenReturn(List.of(feed));

        assertThat(folderService.deleteFolder(folder.getId().toString(), user)).isEqualTo("Folder deleted successfully");

        assertThat(feed.getFolder()).isNull();
        verify(feedRepository).save(feed);
        verify(folderRepository).delete(folder);
    }

    @Test
    void deleteFolderValidatesTheFolder() {
        assertThatThrownBy(() -> folderService.deleteFolder(null, user)).hasMessage("Invalid folder ID");
        assertThatThrownBy(() -> folderService.deleteFolder("bad", user)).hasMessage("Invalid folder ID");

        UUID missing = UUID.randomUUID();
        when(folderRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> folderService.deleteFolder(missing.toString(), user)).hasMessage("Folder not found");

        User other = new User();
        other.setId(UUID.randomUUID());
        Folder foreign = folder("Foreign", other);
        when(folderRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> folderService.deleteFolder(foreign.getId().toString(), user)).hasMessage("Folder doesn't belong to user");
        verify(folderRepository, never()).delete(any());
    }

    @Test
    void updateFolderRenamesIt() {
        Folder folder = folder("News", user);
        when(folderRepository.findById(folder.getId())).thenReturn(Optional.of(folder));
        when(folderRepository.findByNameAndUser("World", user)).thenReturn(Optional.empty());

        assertThat(folderService.updateFolder(folder.getId().toString(), " World ", user)).isEqualTo("Folder updated successfully");

        assertThat(folder.getName()).isEqualTo("World");
        verify(folderRepository).save(folder);
    }

    @Test
    void updateFolderValidatesInput() {
        assertThatThrownBy(() -> folderService.updateFolder(" ", "Name", user)).hasMessage("Invalid folder ID");
        assertThatThrownBy(() -> folderService.updateFolder(UUID.randomUUID().toString(), null, user)).hasMessage("Folder name cannot be empty");
        assertThatThrownBy(() -> folderService.updateFolder("bad", "Name", user)).hasMessage("Invalid folder ID");

        Folder folder = folder("News", user);
        when(folderRepository.findById(folder.getId())).thenReturn(Optional.of(folder));
        when(folderRepository.findByNameAndUser("Tech", user)).thenReturn(Optional.of(folder("Tech", user)));
        assertThatThrownBy(() -> folderService.updateFolder(folder.getId().toString(), "Tech", user)).hasMessage("Folder already exists");
        assertThat(folder.getName()).isEqualTo("News");
    }

    @Test
    void getAllFoldersQueriesTheRepository() {
        List<Folder> all = List.of(folder("A", user));
        when(folderRepository.findAll()).thenReturn(all);
        when(folderRepository.findAllByUser(user)).thenReturn(all);

        assertThat(folderService.getAllFolders()).isSameAs(all);
        assertThat(folderService.getAllFoldersByUser(user)).isSameAs(all);
    }
}
