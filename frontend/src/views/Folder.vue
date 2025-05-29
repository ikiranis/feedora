<template>
    <div class="container py-3">
        <div class="row justify-content-center">
            <div class="col-auto">
                <div class="d-flex align-items-center gap-3">
                    <button 
                        class="btn btn-primary"
                        data-bs-toggle="modal" 
                        data-bs-target="#addFolderModal"
                        style="white-space: nowrap;"
                        :title="language.get('Add a new folder to organize your feeds') || 'Add a new folder to organize your feeds'"
                    >
                        {{ language.get('Add Folder') || 'Add Folder' }}
                    </button>
                    <div class="input-group" style="width: 250px;">
                        <span class="input-group-text">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-search" viewBox="0 0 16 16">
                                <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
                            </svg>
                        </span>
                        <input 
                            v-model="searchTerm"
                            type="text" 
                            class="form-control"
                            :placeholder="language.get('Search folders by name...') || 'Search folders by name...'"
                            :title="language.get('Filter folders by name') || 'Filter folders by name'"
                        />
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="scrollable-folder-view" ref="scrollableContainerRef">
        <div class="container py-4">
            <div v-if="filteredFolders.length > 0" class="table-responsive">
                <table class="table table-bordered table-hover align-middle">
                    <thead class="table-light custom-table-header">
                        <tr>
                            <th>{{ language.get('Folder Name') || 'Folder Name' }}</th>
                            <th>{{ language.get('Feed Count') || 'Feed Count' }}</th>
                            <th>{{ language.get('Actions') || 'Actions' }}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="folder in filteredFolders" :key="folder.id">
                            <td>
                                <div class="d-flex align-items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-folder-fill me-2 text-warning" viewBox="0 0 16 16">
                                        <path d="M9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.825a2 2 0 0 1-1.991-1.819l-.637-7a1.99 1.99 0 0 1 .342-1.31L.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3zm-8.322.12C1.72 3.042 1.95 3 2.19 3h5.396l-.707-.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139z"/>
                                    </svg>
                                    <strong>{{ folder.name }}</strong>
                                </div>
                            </td>
                            <td>
                                <span class="badge bg-secondary">{{ getFeedCount(folder) }}</span>
                            </td>
                            <td>
                                <button 
                                    class="btn btn-danger btn-sm"
                                    @click="deleteFolder(folder)"
                                    :title="language.get('Delete this folder') || 'Delete this folder'"
                                >
                                    <svg width="16" height="16" fill="currentColor" class="bi bi-trash" viewBox="0 0 16 16">
                                        <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5Zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5Zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6Z"/>
                                        <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1ZM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118ZM2.5 3h11V2h-11v1Z"/>
                                    </svg>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div v-else-if="!loading && folders.length === 0 && !searchTerm.trim()" class="text-center text-muted mt-5">
                <div class="mb-3">
                    <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor" class="bi bi-folder2-open text-muted" viewBox="0 0 16 16">
                        <path d="M1 3.5A1.5 1.5 0 0 1 2.5 2h2.764c.958 0 1.76.56 2.311 1.184C7.985 3.648 8.48 4 9 4h4.5A1.5 1.5 0 0 1 15 5.5v.64c.57.265.94.876.856 1.546l-.64 5.124A2.5 2.5 0 0 1 12.733 15H3.266a2.5 2.5 0 0 1-2.481-2.19l-.64-5.124A1.5 1.5 0 0 1 1 6.14V3.5zM2 6h12v-.5a.5.5 0 0 0-.5-.5H9c-.964 0-1.71-.629-2.174-1.154C6.374 3.334 5.82 3 5.264 3H2.5a.5.5 0 0 0-.5.5V6zm-.367 1a.5.5 0 0 0-.496.562l.64 5.124A1.5 1.5 0 0 0 3.266 14h9.468a1.5 1.5 0 0 0 1.489-1.314l.64-5.124A.5.5 0 0 0 14.367 7H1.633z"/>
                    </svg>
                </div>
                <h5>{{ language.get('No folders found') || 'No folders found' }}</h5>
                <p class="text-muted">{{ language.get('Create your first folder to organize your feeds') || 'Create your first folder to organize your feeds' }}</p>
            </div>
            <div v-else-if="!loading && filteredFolders.length === 0 && searchTerm.trim()" class="text-center text-muted mt-5">
                <div class="mb-3">
                    <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor" class="bi bi-search text-muted" viewBox="0 0 16 16">
                        <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
                    </svg>
                </div>
                <h5>{{ language.get('No folders match your search') || 'No folders match your search' }}</h5>
                <p class="text-muted">{{ language.get('Try adjusting your search terms') || 'Try adjusting your search terms' }}</p>
            </div>

            <!-- Fixed loading spinner -->
            <div v-if="loading" class="loading-spinner-fixed">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">{{ language.get('Loading') || 'Loading' }}...</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Add Folder Modal -->
    <AddFolderModal @folder-added="handleFolderAdded" />

    <Error class="error-fixed-bottom" />
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { getFolders, deleteFolder as deleteFolderApi } from '@/api/folder';
import { getFeedsPaginated } from '@/api/feed';
import type { FolderType, Feed } from '@/types';
import { language } from '@/functions/languageStore';
import { errorStore } from '@/components/error/errorStore';
import Error from '@/components/error/Error.vue';
import AddFolderModal from '@/components/folder/AddFolderModal.vue';

const folders = ref<FolderType[]>([]);
const feeds = ref<Feed[]>([]);
const loading = ref(false);
const searchTerm = ref('');
const scrollableContainerRef = ref<HTMLElement | null>(null);

// Computed property for filtered folders
const filteredFolders = computed(() => {
    if (!searchTerm.value.trim()) {
        return folders.value;
    }
    return folders.value.filter(folder => 
        folder.name.toLowerCase().includes(searchTerm.value.toLowerCase())
    );
});

/**
 * Fetches folders from the API
 */
const fetchFolders = async () => {
    try {
        loading.value = true;
        folders.value = await getFolders();
    } catch (error) {
        folders.value = [];
        errorStore.set(true, language.get('Failed to fetch folders') || 'Failed to fetch folders.', 500);
    } finally {
        loading.value = false;
    }
};

/**
 * Fetches feeds to get feed count per folder
 */
const fetchFeeds = async () => {
    try {
        // Get all feeds to count feeds per folder
        feeds.value = await getFeedsPaginated(1, 1000, ''); // Get a large number to get all feeds
    } catch (error) {
        feeds.value = [];
        console.warn('Failed to fetch feeds for folder counts:', error);
    }
};

/**
 * Gets the feed count for a specific folder
 */
const getFeedCount = (folder: FolderType): number => {
    return feeds.value.filter(feed => feed.folderName === folder.name).length;
};

/**
 * Deletes a folder after user confirmation
 */
const deleteFolder = async (folder: FolderType) => {
    const feedCount = getFeedCount(folder);
    let confirmMessage = `${language.get('Are you sure you want to delete this folder') || 'Are you sure you want to delete this folder'}?\n\n"${folder.name}"`;
    
    if (feedCount > 0) {
        confirmMessage += `\n\n${language.get('This folder contains') || 'This folder contains'} ${feedCount} ${language.get('feeds') || 'feeds'}. ${language.get('These feeds will be moved to the default folder') || 'These feeds will be moved to the default folder'}.`;
    }
    
    const confirmed = confirm(confirmMessage);
    
    if (!confirmed) {
        return;
    }
    
    try {
        loading.value = true;
        const result = await deleteFolderApi(folder.id);
        await Promise.all([fetchFolders(), fetchFeeds()]); // Refresh both folders and feeds
        errorStore.set(true, result, 200); // Show success message
    } catch (e: any) {
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to delete folder') || 'Failed to delete folder.', 500);
        }
    } finally {
        loading.value = false;
    }
};

/**
 * Handles when a new folder is added via the modal.
 * Refreshes the folders list to show the new folder.
 */
const handleFolderAdded = async () => {
    await Promise.all([fetchFolders(), fetchFeeds()]); // Refresh both folders and feeds
};

onMounted(async () => {
    // Ensure body overflow is hidden when this component is active
    document.body.style.overflow = 'hidden';
    
    // Load both folders and feeds
    await Promise.all([fetchFolders(), fetchFeeds()]);
});

// Note: If component cleanup is needed in the future:
// onUnmounted(() => { document.body.style.overflow = 'auto'; });
</script>

<style scoped>
/* Make the main view scrollable instead of the whole page */
.scrollable-folder-view {
    height: 80vh; /* Reduced height to account for fixed header section */
    overflow-y: auto;
}

/* Fixed loading spinner */
.loading-spinner-fixed {
    position: fixed;
    bottom: 20px;
    right: 20px;
    z-index: 1000;
    /* Ensure it's above other content */
}

/* Centered error component */
.error-fixed-bottom {
    position: fixed;
    bottom: 5em;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 1000; /* Same z-index as spinner, or adjust as needed */
}

/* Custom table header for dark mode */
[data-bs-theme="dark"] .custom-table-header {
    background-color: #343a40 !important;
    color: #ffffff !important;
}

[data-bs-theme="dark"] .custom-table-header th {
    background-color: #343a40 !important;
    color: #ffffff !important;
    border-color: #495057 !important;
}

/* Folder icon styling */
.bi-folder-fill {
    filter: drop-shadow(0 1px 2px rgba(0,0,0,0.1));
}

/* Table hover effects */
.table-hover tbody tr:hover {
    background-color: var(--bs-table-hover-bg);
}

/* Badge styling */
.badge {
    font-size: 0.75em;
}
</style>
