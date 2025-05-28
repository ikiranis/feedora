<template>
    <!-- Edit Feed Modal -->
    <div class="modal fade" id="editFeedModal" tabindex="-1" aria-labelledby="editFeedModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="editFeedModalLabel">{{ language.get('Edit Feed') }}</h5>
                    <button type="button" class="btn-close" @click="closeModal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form @submit.prevent="saveFeed">
                        <div class="mb-3">
                            <label for="editFeedTitleInput" class="form-label">{{ language.get('Title') }}</label>
                            <input 
                                type="text" 
                                class="form-control" 
                                id="editFeedTitleInput"
                                v-model="feedTitle"
                                required
                            />
                        </div>
                        <div class="mb-3">
                            <label for="editFeedFolderInput" class="form-label">{{ language.get('Folder') }}</label>
                            <select 
                                class="form-select" 
                                id="editFeedFolderInput"
                                v-model="selectedFolderId"
                            >
                                <option value="">{{ language.get('Default') }}</option>
                                <option v-for="folder in folders" :key="folder.id" :value="folder.id">
                                    {{ folder.name }}
                                </option>
                            </select>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" @click="closeModal">
                        {{ language.get('Cancel') }}
                    </button>
                    <button type="button" class="btn btn-primary" @click="saveFeed" :disabled="!feedTitle.trim() || loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        {{ language.get('Save Changes') || 'Save Changes' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { updateFeed as updateFeedApi } from '@/api/feed';
import { getFolders } from '@/api/folder';
import { Feed, FolderType } from '@/types';
import { language } from '@/functions/languageStore';
import { errorStore } from '@/components/error/errorStore';

// Props
interface Props {
    feed?: Feed | null;
    show?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    feed: null,
    show: false
});

// Emits
const emit = defineEmits<{
    'feed-updated': [];
    'modal-hidden': [];
}>();

// Reactive variables
const feedTitle = ref('');
const feedFolder = ref('');
const selectedFolderId = ref('');
const folders = ref<FolderType[]>([]);
const loading = ref(false);
let modalInstance: any = null;

// Watch for feed changes to populate form
watch(() => props.feed, (newFeed) => {
    if (newFeed) {
        feedTitle.value = newFeed.title || '';
        feedFolder.value = newFeed.folderName || '';
        
        // Set the selected folder ID based on folder name
        if (newFeed.folderName) {
            const folder = folders.value.find(f => f.name === newFeed.folderName);
            selectedFolderId.value = folder ? folder.id : '';
        } else {
            selectedFolderId.value = '';
        }
    } else {
        feedTitle.value = '';
        feedFolder.value = '';
        selectedFolderId.value = '';
    }
}, { immediate: true });

// Load folders when component mounts
onMounted(async () => {
    try {
        folders.value = await getFolders();
    } catch (error) {
        console.error('Failed to fetch folders:', error);
        folders.value = [];
    }
});

// Watch for show prop changes to control modal visibility
watch(() => props.show, (show) => {
    if (show) {
        showModal();
    } else {
        hideModal();
    }
});

// Show modal function
const showModal = () => {
    const modalElement = document.getElementById('editFeedModal');
    if (modalElement) {
        try {
            // Try to use Bootstrap's Modal API if available
            if ((window as any).bootstrap && (window as any).bootstrap.Modal) {
                modalInstance = new (window as any).bootstrap.Modal(modalElement);
                modalInstance.show();
                
                // Listen for modal hidden event
                modalElement.addEventListener('hidden.bs.modal', () => {
                    emit('modal-hidden');
                });
            } else {
                // Fallback: manually show modal
                modalElement.classList.add('show');
                modalElement.style.display = 'block';
                modalElement.setAttribute('aria-modal', 'true');
                modalElement.removeAttribute('aria-hidden');
                
                // Add backdrop
                const backdrop = document.createElement('div');
                backdrop.className = 'modal-backdrop fade show';
                backdrop.onclick = () => {
                    hideModal();
                    emit('modal-hidden');
                };
                document.body.appendChild(backdrop);
                
                // Add modal-open class to body
                document.body.classList.add('modal-open');
            }
        } catch (error) {
            console.warn('Bootstrap Modal API not available, using fallback method', error);
            // Fallback method (same as in else block above)
            modalElement.classList.add('show');
            modalElement.style.display = 'block';
            modalElement.setAttribute('aria-modal', 'true');
            modalElement.removeAttribute('aria-hidden');
            
            const backdrop = document.createElement('div');
            backdrop.className = 'modal-backdrop fade show';
            backdrop.onclick = () => {
                hideModal();
                emit('modal-hidden');
            };
            document.body.appendChild(backdrop);
            
            document.body.classList.add('modal-open');
        }
    }
};

// Close modal function
const closeModal = () => {
    hideModal();
    emit('modal-hidden');
};

// Hide modal function
const hideModal = () => {
    const modalElement = document.getElementById('editFeedModal');
    if (modalElement) {
        try {
            // Try to use Bootstrap's Modal API if available
            if (modalInstance && (window as any).bootstrap && (window as any).bootstrap.Modal) {
                modalInstance.hide();
                modalInstance = null;
            } else {
                // Fallback: manually hide modal
                modalElement.classList.remove('show');
                modalElement.style.display = 'none';
                modalElement.setAttribute('aria-hidden', 'true');
                modalElement.removeAttribute('aria-modal');
                
                // Remove backdrop if it exists
                const backdrop = document.querySelector('.modal-backdrop');
                if (backdrop) {
                    backdrop.remove();
                }
                
                // Remove modal-open class from body
                document.body.classList.remove('modal-open');
                document.body.style.overflow = '';
                document.body.style.paddingRight = '';
            }
        } catch (error) {
            console.warn('Bootstrap Modal API not available, using fallback method', error);
            // Fallback method
            modalElement.classList.remove('show');
            modalElement.style.display = 'none';
            modalElement.setAttribute('aria-hidden', 'true');
            modalElement.removeAttribute('aria-modal');
            
            const backdrop = document.querySelector('.modal-backdrop');
            if (backdrop) {
                backdrop.remove();
            }
            
            document.body.classList.remove('modal-open');
            document.body.style.overflow = '';
            document.body.style.paddingRight = '';
        }
    }
};

// Save feed function
const saveFeed = async () => {
    if (!props.feed || !feedTitle.value.trim()) return;
    
    try {
        loading.value = true;
        
        // Get folder name from selected folder ID
        let folderName = '';
        if (selectedFolderId.value) {
            const selectedFolder = folders.value.find(f => f.id === selectedFolderId.value);
            folderName = selectedFolder ? selectedFolder.name : '';
        }
        
        const result = await updateFeedApi(props.feed.id, {
            title: feedTitle.value.trim(),
            folderName: folderName
        });
        
        hideModal();
        emit('modal-hidden');
        emit('feed-updated');
        errorStore.set(true, result, 200);
    } catch (e: any) {
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to update feed') || 'Failed to update feed.', 500);
        }
    } finally {
        loading.value = false;
    }
};
</script>
