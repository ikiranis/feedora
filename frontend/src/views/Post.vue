<template>
    <div class="container py-4">
        <div class="row justify-content-between mb-4">
            <div class="col-auto">
                <h2>{{ language.get('Posts') }}</h2>
            </div>
            <div class="col-auto">
                <button @click="handleParseFeeds" class="btn btn-primary me-2">
                    {{ language.get('Parse RSS Feeds') }}
                </button>
                <button @click="handleDeleteAllPosts" class="btn btn-danger">
                    {{ language.get('Delete All Posts') }}
                </button>
            </div>
        </div>
        <div v-if="error" class="alert alert-danger text-center mt-3">{{ error }}</div>
        <div v-if="posts.length > 0" class="table-responsive">
            <table class="table table-bordered table-hover align-middle">
                <thead class="table-light">
                    <tr>
                        <th>{{ language.get('Title') }}</th>
                        <th>{{ language.get('Author') }}</th>
                        <th>{{ language.get('Date') }}</th>
                        <th>{{ language.get('Feed') }}</th>
                        <th>{{ language.get('Read') }}</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="post in posts" :key="post.id">
                        <td>
                            <a :href="post.link" target="_blank">{{ post.title }}</a>
                        </td>
                        <td>{{ post.author }}</td>
                        <td>{{ post.pubDate ? new Date(post.pubDate).toLocaleString() : '' }}</td>
                        <td>{{ post.feed?.title }}</td>
                        <td>
                            <span v-if="post.read" class="badge bg-success">{{ language.get('Yes') }}</span>
                            <span v-else class="badge bg-secondary">{{ language.get('No') }}</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div v-else-if="!error" class="text-center text-muted mt-5">
            {{ language.get('No posts found') }}
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getPosts, parseFeeds, deleteAllPosts } from '@/api/post';
import { PostType } from '@/types';
import { language } from '@/functions/languageStore';

const posts = ref<PostType[]>([]);
const error = ref('');

const fetchPosts = async () => {
    try {
        error.value = '';
        const data = await getPosts();
        posts.value = data;
    } catch (e) {
        error.value = 'Failed to fetch posts.';
    }
};

const handleParseFeeds = async () => {
    try {
        error.value = '';
        const result = await parseFeeds();
        await fetchPosts();
        alert(result);
    } catch (e) {
        error.value = 'Failed to parse feeds.';
    }
};

const handleDeleteAllPosts = async () => {
    try {
        error.value = '';
        const result = await deleteAllPosts();
        await fetchPosts();
        alert(result);
    } catch (e) {
        error.value = 'Failed to delete posts.';
    }
};

onMounted(fetchPosts);
</script>

<style scoped>
/* No custom styles needed, using Bootstrap classes */
</style>
