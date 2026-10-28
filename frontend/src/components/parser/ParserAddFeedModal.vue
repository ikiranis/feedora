<template>
    <Teleport to="body">
        <div class="modal fade" id="parserAddFeedModal" tabindex="-1" aria-labelledby="parserAddFeedModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="parserAddFeedModalLabel">{{ language.get('Add this feed to your feeds') }}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <form @submit.prevent="add">
                            <div class="mb-3">
                                <label class="form-label">{{ language.get('Feed Title') }}</label>
                                <div class="form-control-plaintext py-0">{{ page?.title }}</div>
                            </div>
                            <div class="mb-3">
                                <label for="parserFeedFolder" class="form-label">{{ language.get('Select Folder') }}</label>
                                <select id="parserFeedFolder" class="form-select" v-model="selectedFolderId">
                                    <option value="">{{ language.get('Default') }}</option>
                                    <option v-for="folder in folders" :key="folder.id" :value="folder.id">
                                        {{ folder.name }}
                                    </option>
                                </select>
                            </div>
                            <div v-if="errorMessage" class="alert alert-danger mb-0">{{ errorMessage }}</div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            {{ language.get('Close') }}
                        </button>
                        <button type="button" class="btn btn-primary" @click="add" :disabled="adding">
                            <span v-if="adding" class="spinner-border spinner-border-sm me-2" role="status"></span>
                            {{ language.get('Add') }}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { language } from '@/functions/languageStore';
import { getFolders } from '@/api/folder';
import { addParsePageToFeeds } from '@/api/parser';
import type { FolderType, ParsePageType } from '@/types';

interface Props {
    page?: ParsePageType | null;
    show?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    page: null,
    show: false
});

const emit = defineEmits<{
    'added': [page: ParsePageType, message: string];
    'modal-hidden': [];
}>();

const folders = ref<FolderType[]>([]);
// Empty for the default folder
const selectedFolderId = ref('');
const adding = ref(false);
const errorMessage = ref('');

const modalElement = () => document.getElementById('parserAddFeedModal');

/**
 * Folders may have changed since the last time, so they're loaded every time the modal opens
 */
const loadFolders = async () => {
    try {
        folders.value = await getFolders();
    } catch (error) {
        console.error('Failed to fetch folders:', error);
        folders.value = [];
    }
};

watch(() => props.show, (show) => {
    const element = modalElement();
    if (!element) return;

    if (show) {
        selectedFolderId.value = '';
        errorMessage.value = '';
        loadFolders();
        (window as any).bootstrap.Modal.getOrCreateInstance(element).show();
    } else {
        (window as any).bootstrap.Modal.getInstance(element)?.hide();
    }
});

const handleHidden = () => emit('modal-hidden');

onMounted(() => modalElement()?.addEventListener('hidden.bs.modal', handleHidden));

onUnmounted(() => {
    const element = modalElement();
    element?.removeEventListener('hidden.bs.modal', handleHidden);
    (window as any).bootstrap.Modal.getInstance(element)?.dispose();
});

const add = async () => {
    if (!props.page || adding.value) return;

    try {
        adding.value = true;
        errorMessage.value = '';
        const message = await addParsePageToFeeds(props.page.id, selectedFolderId.value || null);
        emit('added', props.page, message);
        (window as any).bootstrap.Modal.getInstance(modalElement())?.hide();
    } catch (error: any) {
        errorMessage.value = typeof error.response?.data === 'string' && error.response.data
            ? error.response.data
            : language.get('Failed to add feed');
    } finally {
        adding.value = false;
    }
};
</script>
