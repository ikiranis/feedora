<template>
    <div class="modal fade" id="addFeedModal" tabindex="-1" aria-labelledby="addFeedModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="addFeedModalLabel">{{ language.get('Add Feed') }}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form @submit.prevent="handleAddFeed">
                        <div class="mb-3">
                            <label for="feedUrl" class="form-label">{{ language.get('Feed URL') }}</label>
                            <input 
                                type="url" 
                                class="form-control" 
                                id="feedUrl"
                                v-model="feedUrl"
                                :placeholder="language.get('Enter the RSS feed URL')"
                                @blur="fetchFeedTitle"
                                required
                            />
                        </div>
                        
                        <div class="mb-3">
                            <label for="feedTitle" class="form-label">{{ language.get('Feed Title') }}</label>
                            <input 
                                type="text" 
                                class="form-control" 
                                id="feedTitle"
                                v-model="feedTitle"
                                :placeholder="language.get('Auto-detected title')"
                                readonly
                            />
                            <div v-if="fetchingTitle" class="form-text text-muted">
                                <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                                {{ language.get('Fetching feed title...') }}
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="feedFolder" class="form-label">{{ language.get('Select Folder') }}</label>
                            <select 
                                class="form-select" 
                                id="feedFolder"
                                v-model="selectedFolderId"
                                required
                            >
                                <option value="">{{ language.get('Choose a folder for this feed') }}</option>
                                <option value="default">{{ language.get('Default') }}</option>
                                <option v-for="folder in folders" :key="folder.id" :value="folder.id">
                                    {{ folder.name }}
                                </option>
                            </select>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                        {{ language.get('Close') }}
                    </button>
                    <button 
                        type="button" 
                        class="btn btn-primary" 
                        @click="handleAddFeed"
                        :disabled="!feedUrl || !selectedFolderId || adding"
                    >
                        <div v-if="adding" class="spinner-border spinner-border-sm me-2" role="status"></div>
                        {{ language.get('Add') }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { language } from '@/functions/languageStore';
import { getFolders } from '@/api/folder';
import { addFeed, fetchFeedInfo } from '@/api/feed';
import { errorStore } from '@/components/error/errorStore';
import type { FolderType } from '@/types';

const feedUrl = ref('');
const feedTitle = ref('');
const selectedFolderId = ref('');
const folders = ref<FolderType[]>([]);
const adding = ref(false);
const fetchingTitle = ref(false);

const emit = defineEmits<{
    'feed-added': []
}>();

/**
 * Helper function to properly close the modal
 */
const closeModal = () => {
    const modal = document.getElementById('addFeedModal');
    if (!modal) return;
    
    try {
        // Try to use Bootstrap's Modal API if available
        if ((window as any).bootstrap && (window as any).bootstrap.Modal) {
            const bootstrapModal = (window as any).bootstrap.Modal.getInstance(modal);
            if (bootstrapModal) {
                bootstrapModal.hide();
                return;
            } else {
                // Create new instance and hide
                const newModal = new (window as any).bootstrap.Modal(modal);
                newModal.hide();
                return;
            }
        }
    } catch (error) {
        console.warn('Bootstrap Modal API not available, using fallback method');
    }
    
    // Fallback: manually hide modal
    modal.classList.remove('show');
    modal.style.display = 'none';
    modal.setAttribute('aria-hidden', 'true');
    modal.removeAttribute('aria-modal');
    
    // Remove backdrop if it exists
    const backdrop = document.querySelector('.modal-backdrop');
    if (backdrop) {
        backdrop.remove();
    }
    
    // Remove modal-open class from body
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
};

/**
 * Fetches the list of folders when the component is mounted
 */
onMounted(async () => {
    try {
        folders.value = await getFolders();
    } catch (error) {
        console.error('Failed to fetch folders:', error);
        folders.value = [];
    }
});

/**
 * Fetches the feed title from the provided URL
 */
const fetchFeedTitle = async () => {
    if (!feedUrl.value || fetchingTitle.value) return;
    
    try {
        fetchingTitle.value = true;
        const feedInfo = await fetchFeedInfo(feedUrl.value);
        feedTitle.value = feedInfo.title || '';
    } catch (error) {
        console.error('Failed to fetch feed title:', error);
        errorStore.set(true, language.get('Failed to fetch feed title') || 'Failed to fetch feed title', 500);
    } finally {
        fetchingTitle.value = false;
    }
};

/**
 * Handles adding a new feed
 */
const handleAddFeed = async () => {
    if (!feedUrl.value || !selectedFolderId.value) {
        if (!feedUrl.value) {
            errorStore.set(true, language.get('Please enter a valid feed URL') || 'Please enter a valid feed URL', 400);
        } else {
            errorStore.set(true, language.get('Please select a folder') || 'Please select a folder', 400);
        }
        return;
    }

    try {
        adding.value = true;
        
        await addFeed({
            url: feedUrl.value,
            folderId: selectedFolderId.value === 'default' ? null : selectedFolderId.value,
            title: feedTitle.value
        });

        errorStore.set(true, language.get('Feed added successfully') || 'Feed added successfully', 200);
        
        // Reset form
        feedUrl.value = '';
        feedTitle.value = '';
        selectedFolderId.value = '';
        
        // Emit event to parent to refresh feeds
        emit('feed-added');
        
        // Close modal
        closeModal();
        
    } catch (error: any) {
        console.error('Failed to add feed:', error);
        let errorMessage = language.get('Failed to add feed') || 'Failed to add feed';
        
        if (error.response && error.response.data) {
            // Backend returns plain string error messages
            errorMessage = error.response.data;
        } else if (error.message) {
            errorMessage = error.message;
        }
        
        errorStore.set(true, errorMessage, error.response?.status || 500);
    } finally {
        adding.value = false;
    }
};
</script>

<style scoped>
.modal-body {
    max-height: 60vh;
    overflow-y: auto;
}

.spinner-border-sm {
    width: 1rem;
    height: 1rem;
}
</style>
