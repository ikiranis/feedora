<template>
    <div class="container py-4">
        <div class="row justify-content-center mb-4">
            <div class="col-auto">
                <div v-if="feeds.length === 0" class="text-center">
                    <input 
                        ref="fileInput"
                        type="file" 
                        accept=".opml,.xml" 
                        @change="handleFileSelect"
                        class="form-control mb-3"
                        style="max-width: 300px; display: inline-block;"
                    />
                    <br>
                    <button 
                        @click="importOPML" 
                        :disabled="!selectedFile"
                        class="btn btn-success btn-lg"
                    >
                        Import OPML
                    </button>
                </div>
            </div>
        </div>
        <div v-if="feeds.length > 0" class="table-responsive">
            <table class="table table-bordered table-hover align-middle">
                <thead class="table-light">
                    <tr>
                        <th>Title</th>
                        <th>XML URL</th>
                        <th>HTML URL</th>
                        <th>Type</th>
                        <th>Folder</th>
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
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getFeeds, importOPML as importOPMLApi } from '@/api/feed';
import { Feed } from '@/types';

const feeds = ref<Feed[]>([]);
const error = ref('');
const selectedFile = ref<File | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);

const fetchFeeds = async () => {
    try {
        const data = await getFeeds();
        feeds.value = data;
    } catch (e) {
        error.value = 'Failed to fetch feeds.';
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
        error.value = 'Please select an OPML file first.';
        return;
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
        error.value = 'Failed to import OPML.';
    }
};

onMounted(fetchFeeds);
</script>

<style scoped>
/* No custom styles needed, using Bootstrap classes */
</style>
