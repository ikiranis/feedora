<template>
    <div class="scrollable-feed-view" ref="scrollableContainerRef">
        <div class="container py-4">
            <div v-if="feeds.length > 0" class="table-responsive">
                <table class="table table-bordered table-hover align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>{{ language.get('Title') }}</th>
                            <th>{{ language.get('XML URL') }}</th>
                            <th>{{ language.get('HTML URL') }}</th>
                            <th>{{ language.get('Type') }}</th>
                            <th>{{ language.get('Folder') }}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="feed in feeds" :key="feed.id">
                            <td>{{ feed.title }}</td>
                            <td>{{ feed.xmlUrl }}</td>
                            <td>{{ feed.htmlUrl }}</td>
                            <td>{{ feed.type }}</td>
                            <td>{{ feed.folderName }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div v-else-if="!loading" class="text-center text-muted mt-5">
                {{ language.get('No feeds found') }}
            </div>
            
            <!-- OPML Import Section -->
            <div class="row justify-content-center mt-4">
                <div class="col-auto">
                    <div class="d-flex align-items-center gap-3">
                        <input 
                            ref="fileInput"
                            type="file" 
                            accept=".opml,.xml" 
                            @change="handleFileSelect"
                            class="form-control"
                            style="width: 300px;"
                            :disabled="importing"
                        />
                        <button 
                            @click="importOPML" 
                            :disabled="!selectedFile || importing"
                            class="btn btn-success"
                            style="white-space: nowrap;"
                        >
                            {{ language.get('Import OPML') }}
                        </button>
                    </div>
                </div>
            </div>

            <!-- Fixed loading spinner -->
            <div v-if="loading || importing" class="loading-spinner-fixed">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        </div>
    </div>

    <Error class="error-fixed-bottom" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { getFeeds, importOPML as importOPMLApi, getFeedOperationStatus } from '@/api/feed';
import { Feed } from '@/types';
import { language } from '@/functions/languageStore';
import Error from '@/components/error/Error.vue';
import { errorStore } from '@/components/error/errorStore';

const feeds = ref<Feed[]>([]);
const selectedFile = ref<File | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);
const scrollableContainerRef = ref<HTMLElement | null>(null); // Ref for the scrollable container
const loading = ref(false);
const importing = ref(false);

const fetchFeeds = async () => {
    try {
        loading.value = true;
        const data = await getFeeds();
        feeds.value = data;
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
        await fetchFeeds();
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

onMounted(() => {
    loading.value = true;
    fetchFeeds();
    // Ensure body overflow is hidden when this component is active
    document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
    // Restore body overflow when this component is destroyed
    document.body.style.overflow = 'auto';
});
</script>

<style scoped>
/* Make the main view scrollable instead of the whole page */
.scrollable-feed-view {
    height: 90vh; /* Adjust height as needed */
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
</style>
