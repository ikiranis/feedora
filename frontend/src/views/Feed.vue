<template>
    <div class="container py-4">
        <div class="row justify-content-center mb-4">
            <div class="col-auto">
                <button v-if="feeds.length === 0" @click="importOPML" class="btn btn-success btn-lg">Import
                    OPML</button>
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

const feeds = ref<Feed[]>([]);
const error = ref('');

const fetchFeeds = async () => {
    try {
        const data = await getFeeds();
        feeds.value = data;
    } catch (e) {
        error.value = 'Failed to fetch feeds.';
    }
};

const importOPML = async () => {
    try {
        error.value = '';
        const result = await importOPMLApi();
        await fetchFeeds();
        alert(result);
    } catch (e) {
        error.value = 'Failed to import OPML.';
    }
};

onMounted(fetchFeeds);
</script>

<style scoped>
/* No custom styles needed, using Bootstrap classes */
</style>
