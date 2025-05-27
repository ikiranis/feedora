<template>
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
        <div v-if="error" class="alert alert-danger text-center mt-3">{{ error }}</div>
        
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
                    />
                    <button 
                        @click="importOPML" 
                        :disabled="!selectedFile"
                        class="btn btn-success"
                        style="white-space: nowrap;"
                    >
                        {{ language.get('Import OPML') }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getFeeds, importOPML as importOPMLApi } from '@/api/feed';
import { Feed } from '@/types';
import { language } from '@/functions/languageStore';

const feeds = ref<Feed[]>([]);
const error = ref('');
const selectedFile = ref<File | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);

const fetchFeeds = async () => {
    try {
        const data = await getFeeds();
        feeds.value = data;
    } catch (e) {
        error.value = language.get('Failed to fetch feeds');
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
        error.value = language.get('Please select an OPML file first');
        return;
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
        error.value = '';
        const result = await importOPMLApi(selectedFile.value);
        await fetchFeeds();
        alert(result);
        // Reset file input
        selectedFile.value = null;
        if (fileInput.value) {
            fileInput.value.value = '';
        }
    } catch (e) {
        error.value = language.get('Failed to import OPML');
    }
};

onMounted(fetchFeeds);
</script>

<style scoped>
/* No custom styles needed, using Bootstrap classes */
</style>
