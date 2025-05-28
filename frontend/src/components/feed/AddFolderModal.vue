<template>
    <!-- Add Folder Modal -->
    <div class="modal fade" id="addFolderModal" tabindex="-1" aria-labelledby="addFolderModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="addFolderModalLabel">{{ language.get('Add Folder') || 'Add Folder' }}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form @submit.prevent="addFolder">
                        <div class="mb-3">
                            <label for="folderNameInput" class="form-label">{{ language.get('Folder Name') || 'Folder Name' }}</label>
                            <input 
                                type="text" 
                                class="form-control" 
                                id="folderNameInput"
                                v-model="folderName"
                                :placeholder="language.get('Enter folder name') || 'Enter folder name'"
                                required
                            />
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                        {{ language.get('Cancel') || 'Cancel' }}
                    </button>
                    <button type="button" class="btn btn-primary" @click="addFolder" :disabled="!folderName.trim() || loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        {{ language.get('Add') || 'Add' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { addFolder as addFolderApi } from '@/api/folder';
import { language } from '@/functions/languageStore';
import { errorStore } from '@/components/error/errorStore';

// Emits
const emit = defineEmits<{
    'folder-added': [];
}>();

// Reactive variables
const folderName = ref('');
const loading = ref(false);

// Add folder function
const addFolder = async () => {
    if (!folderName.value.trim()) return;
    
    try {
        loading.value = true;
        const result = await addFolderApi({
            name: folderName.value.trim()
        });
        
        // Hide the modal
        const modalElement = document.getElementById('addFolderModal');
        if (modalElement) {
            try {
                // Try to use Bootstrap's Modal API if available
                if ((window as any).bootstrap && (window as any).bootstrap.Modal) {
                    const bootstrapModal = (window as any).bootstrap.Modal.getInstance(modalElement) || 
                                         new (window as any).bootstrap.Modal(modalElement);
                    bootstrapModal.hide();
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
        
        // Reset form
        folderName.value = '';
        
        // Emit event to parent
        emit('folder-added');
        
        // Show success message
        errorStore.set(true, result, 200);
    } catch (e: any) {
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to add folder') || 'Failed to add folder.', 500);
        }
    } finally {
        loading.value = false;
    }
};
</script>
