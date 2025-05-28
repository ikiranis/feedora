<template>
    <!-- Actio                    <button 
                        @click="importOPML" 
                        :disabled="!selectedFile || importing"
                        class="btn btn-success"
                        style="white-space: nowrap;"
                        :title="language.get('Import feeds from the selected OPML file')"
                    >
                        {{ language.get('Import OPML') }}
                    </button>
                    <input 
                        v-model="searchTerm"
                        type="text" 
                        class="form-control"
                        style="width: 250px;"
                        :placeholder="language.get('Search feeds by name...')"
                        :title="language.get('Filter feeds by name')"
                    />
                </div> - Fixed at top, always visible -->
    <div class="container py-3">
        <div class="row justify-content-center">
            <div class="col-auto">
                <div class="d-flex align-items-center gap-3">
                    <button 
                        class="btn btn-primary"
                        data-bs-toggle="modal" 
                        data-bs-target="#addFeedModal"
                        style="white-space: nowrap;"
                        :title="language.get('Add a new RSS feed to your collection')"
                    >
                        {{ language.get('Add Feed') }}
                    </button>
                    <input 
                        ref="fileInput"
                        type="file" 
                        accept=".opml,.xml" 
                        @change="handleFileSelect"
                        class="form-control"
                        style="width: 300px;"
                        :disabled="importing"
                        :title="language.get('Select an OPML file to import feeds')"
                    />
                    <button 
                        @click="importOPML" 
                        :disabled="!selectedFile || importing"
                        class="btn btn-success"
                        style="white-space: nowrap;"
                        :title="language.get('Import feeds from the selected OPML file')"
                    >
                        {{ language.get('Import OPML') }}
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
                            :placeholder="language.get('Search feeds by name...')"
                            :title="language.get('Filter feeds by name')"
                        />
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="scrollable-feed-view" ref="scrollableContainerRef">
        <div class="container py-4">
            <div v-if="feeds.length > 0" class="table-responsive">
                <table class="table table-bordered table-hover align-middle">
                    <thead class="table-light custom-table-header">
                        <tr>
                            <th>{{ language.get('Title') }}</th>
                            <th>{{ language.get('XML URL') }}</th>
                            <th>{{ language.get('Folder') }}</th>
                            <th>{{ language.get('Actions') }}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="feed in feeds" :key="feed.id">
                            <td>{{ feed.title }}</td>
                            <td>
                                <a :href="feed.xmlUrl" target="_blank" class="text-decoration-none" :title="feed.xmlUrl">
                                    {{ feed.xmlUrl.length > 30 ? feed.xmlUrl.substring(0, 30) + '...' : feed.xmlUrl }}
                                </a>
                            </td>
                            <td>{{ feed.folderName }}</td>
                            <td>
                                <button 
                                    class="btn btn-danger btn-sm"
                                    @click="deleteFeed(feed)"
                                    :title="language.get('Delete this feed')"
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
                <div v-if="allLoaded && feeds.length > 0 && !searchTerm.trim()" class="text-center w-100 text-muted my-4">
                    {{ language.get('No more feeds') || 'No more feeds' }}
                </div>
            </div>
            <div v-else-if="!loading && feeds.length === 0 && !searchTerm.trim()" class="text-center text-muted mt-5">
                {{ language.get('No feeds found') }}
            </div>
            <div v-else-if="!loading && feeds.length === 0 && searchTerm.trim()" class="text-center text-muted mt-5">
                {{ language.get('No feeds match your search') || 'No feeds match your search' }}
            </div>

            <!-- Fixed loading spinner -->
            <div v-if="loading || importing" class="loading-spinner-fixed">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Add Feed Modal -->
    <AddFeedModal @feed-added="handleFeedAdded" />

    <Error class="error-fixed-bottom" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { getFeedsPaginated, importOPML as importOPMLApi, getFeedOperationStatus, deleteFeed as deleteFeedApi } from '@/api/feed';
import { Feed } from '@/types';
import { language } from '@/functions/languageStore';
import Error from '@/components/error/Error.vue';
import AddFeedModal from '@/components/AddFeedModal.vue';
import { errorStore } from '@/components/error/errorStore';

const feeds = ref<Feed[]>([]);
const selectedFile = ref<File | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);
const scrollableContainerRef = ref<HTMLElement | null>(null); // Ref for the scrollable container
const loading = ref(false);
const importing = ref(false);
const page = ref(1);
const pageSize = 15;
const allLoaded = ref(false);
const searchTerm = ref('');
const searchTimeout = ref<number | null>(null);

// Remove the client-side filteredFeeds computed property since we'll use server-side search
// The feeds array will now contain the already filtered results from the server

/**
 * Fetches feeds from the API. It supports pagination and resetting the feed list.
 * @param {boolean} [reset=false] - If true, clears existing feeds and resets pagination.
 */
const fetchFeeds = async (reset = false) => {
    try {
        if (reset) {
            feeds.value = [];
            page.value = 1;
            allLoaded.value = false;
        }

        const data = await getFeedsPaginated(page.value, pageSize, searchTerm.value);

        if (data.length < pageSize) {
            allLoaded.value = true;
        }

        if (reset) {
            feeds.value = data;
        } else {
            feeds.value = feeds.value.concat(data);
        }

        page.value++; // Increment page for the next fetch
    } catch (e: any) {
        if (e.response && e.response.data && e.response.data.message && typeof e.response.data.status === 'number') {
            errorStore.set(true, e.response.data.message, e.response.data.status);
        } else {
            errorStore.set(true, language.get('Failed to fetch feeds') || 'Failed to fetch feeds.', 500);
        }
    } finally {
        loading.value = false;
    }
};

// Watch for search term changes and implement debounced search
watch(searchTerm, () => {
    // Clear the existing timeout
    if (searchTimeout.value) {
        clearTimeout(searchTimeout.value);
    }
    
    // Set a new timeout for debounced search (500ms delay)
    searchTimeout.value = setTimeout(() => {
        fetchFeeds(true); // Reset and fetch with new search term
    }, 500);
});

const handleFileSelect = (event: Event) => {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
        selectedFile.value = target.files[0];
    }
};

const importOPML = async () => {
    if (!selectedFile.value) {
        errorStore.set(true, language.get('Please select an OPML file first') || 'Please select an OPML file first.', 400);
        return;
    }
    
    try {
        // Check if a feed operation is already running
        const statusResponse = await getFeedOperationStatus();
        if (statusResponse.isRunning) {
            errorStore.set(true, language.get('Cannot import OPML: Feed parsing operation is currently in progress. Please try again in a few moments.') || 'Cannot import OPML: Feed parsing operation is currently in progress. Please try again in a few moments.', 409);
            return;
        }
    } catch (e: any) {
        console.warn('Could not check feed operation status:', e);
        // Continue with import attempt even if status check fails
    }
    
    // Show confirmation dialog
    const confirmed = confirm(
        `⚠️ ${language.get('IMPORTANT WARNING')} ⚠️\n\n` +
        `${language.get('Importing this OPML file will')}\n` +
        `• ${language.get('DELETE all your existing feeds')}\n` +
        `• ${language.get('DELETE all your existing folders')}\n` +
        `• ${language.get('DELETE all your existing posts')}\n\n` +
        `${language.get('This action CANNOT be undone')}\n\n` +
        `${language.get('Are you sure you want to continue')}`
    );
    
    if (!confirmed) {
        return; // User cancelled the operation
    }
    
    try {
        importing.value = true;
        const result = await importOPMLApi(selectedFile.value);
        await fetchFeeds(true); // Refresh feeds with reset
        errorStore.set(true, result, 200); // Use errorStore for success message
        // Reset file input
        selectedFile.value = null;
        if (fileInput.value) {
            fileInput.value.value = '';
        }
    } catch (e: any) {
        if (e.response && e.response.data && e.response.data.message && typeof e.response.data.status === 'number') {
            errorStore.set(true, e.response.data.message, e.response.data.status);
        } else {
            errorStore.set(true, language.get('Failed to import OPML') || 'Failed to import OPML.', 500);
        }
    } finally {
        importing.value = false;
    }
};

/**
 * Deletes a feed after user confirmation
 */
const deleteFeed = async (feed: Feed) => {
    const confirmed = confirm(
        `${language.get('Are you sure you want to delete this feed')}\n\n` +
        `${feed.title || 'Untitled Feed'}`
    );
    
    if (!confirmed) {
        return;
    }
    
    try {
        loading.value = true;
        const result = await deleteFeedApi(feed.id);
        await fetchFeeds(true); // Refresh feeds with reset
        errorStore.set(true, result, 200); // Show success message
    } catch (e: any) {
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to delete feed') || 'Failed to delete feed.', 500);
        }
    } finally {
        loading.value = false;
    }
};

/**
 * Handles the scroll event on the scrollable container.
 * When the user scrolls near the bottom, it loads more feeds if available.
 */
const handleScroll = () => {
    if (loading.value || allLoaded.value || !scrollableContainerRef.value) return;

    const container = scrollableContainerRef.value;
    const scrollTop = container.scrollTop;
    const clientHeight = container.clientHeight;
    const scrollHeight = container.scrollHeight;

    // Load more when 200px from the bottom
    if (scrollTop + clientHeight + 200 >= scrollHeight) {
        loading.value = true;
        fetchFeeds();
    }
};

/**
 * Handles when a new feed is added via the modal.
 * Refreshes the feeds list to show the new feed.
 */
const handleFeedAdded = async () => {
    loading.value = true;
    await fetchFeeds(true); // Refresh feeds
};

onMounted(() => {
    loading.value = true;
    fetchFeeds(true); // Initial fetch
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.addEventListener('scroll', handleScroll);
    }
    // Ensure body overflow is hidden when this component is active
    document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.removeEventListener('scroll', handleScroll);
    }
    // Restore body overflow when this component is destroyed
    document.body.style.overflow = 'auto';
});
</script>

<style scoped>
/* Make the main view scrollable instead of the whole page */
.scrollable-feed-view {
    height: 80vh; /* Reduced height to account for fixed import section */
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
</style>
