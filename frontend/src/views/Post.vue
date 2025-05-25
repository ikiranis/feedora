<template>
    <div class="container py-4">
        <div class="row justify-content-between mb-4 align-items-center">
            <div class="col-auto">
                <h2>{{ language.get('Posts') }}</h2>
            </div>
            <div class="col-auto d-flex align-items-center">
                <button @click="showDetails = !showDetails" class="btn btn-outline-secondary me-3" 
                        :title="showDetails ? language.get('Show Summary') : language.get('Show Details')">
                    <svg v-if="showDetails" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-list" viewBox="0 0 16 16">
                        <title>{{ language.get('Show Summary') }}</title>
                        <path fill-rule="evenodd" d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5z"/>
                    </svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-card-text" viewBox="0 0 16 16">
                        <title>{{ language.get('Show Details') }}</title>
                        <path d="M14.5 3a.5.5 0 0 1 .5.5v9a.5.5 0 0 1-.5.5h-13a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5h13zm-13-1A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h13a1.5 1.5 0 0 0 1.5-1.5v-9A1.5 1.5 0 0 0 14.5 2h-13z"/>
                        <path d="M3 5.5a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9a.5.5 0 0 1-.5-.5zM3 8a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9A.5.5 0 0 1 3 8zm0 2.5a.5.5 0 0 1 .5-.5h6a.5.5 0 0 1 0 1h-6a.5.5 0 0 1-.5-.5z"/>
                    </svg>
                </button>
                <button @click="handleParseFeeds" class="btn btn-primary me-2">
                    {{ language.get('Parse RSS Feeds') }}
                </button>
                <button @click="handleDeleteAllPosts" class="btn btn-danger">
                    {{ language.get('Delete All Posts') }}
                </button>
            </div>
        </div>
        <div v-if="error" class="alert alert-danger text-center mt-3">{{ error }}</div>
        <div v-if="posts.length > 0" class="row g-4">
            <div v-for="post in posts" :key="post.id" :data-post-id="post.id" 
                 :class="showDetails ? 'col-12 col-md-6 col-lg-4' : 'col-12'">
                <PostElement :post="post" :show-details="showDetails" />
            </div>
            <div v-if="allLoaded && posts.length > 0" class="text-center w-100 text-muted my-4">
                {{ language.get('No more posts') || 'No more posts' }}
            </div>
        </div>
        <div v-else-if="!error" class="text-center text-muted mt-5">
            {{ language.get('No posts found') }}
        </div>

        <!-- Fixed bottom-right loading spinner -->
        <div v-if="loading" class="loading-spinner-fixed">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>

    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { getPosts, parseFeeds, deleteAllPosts } from '@/api/post';
import { PostType } from '@/types';
import { language } from '@/functions/languageStore';
import PostElement from '@/components/PostElement.vue';

const posts = ref<PostType[]>([]);
const error = ref('');
const page = ref(1);
const pageSize = 15;
const loading = ref(false);
const allLoaded = ref(false);
const showDetails = ref(true); // Added for checkbox

// Remove all size-related attributes (width, height, srcset, sizes, style with width/height) from <img> tags in HTML string and add a class for styling
const removeImgSizeAttrsFromHtml = (html: string): string => {
    // Remove size-related attributes
    let cleaned = html
        .replace(/\s(width|height|srcset|sizes)="[^"]*"/gi, '')
        .replace(/\sstyle="[^"]*(width|height)[^"]*"/gi, '');
    // Add class to all <img> tags (preserve existing classes)
    cleaned = cleaned.replace(/<img(\s[^>]*?)?>/gi, (match) => {
        if (/class\s*=/.test(match)) {
            return match.replace(/class\s*=\s*"([^"]*)"/i, 'class="$1 post-img"');
        } else {
            return match.replace('<img', '<img class="post-img"');
        }
    });
    return cleaned;
};

const fetchPosts = async (reset = false) => {
    // Assumes loading.value is true, set by the caller.
    try {
        error.value = '';
        if (reset) {
            posts.value = [];
            page.value = 1;
            allLoaded.value = false;
        }

        const data = await getPosts();
        // Remove width/height from images in post.description
        data.forEach((post: PostType) => {
            if (post.description) {
                post.description = removeImgSizeAttrsFromHtml(post.description);
            }
        });

        const start = (page.value - 1) * pageSize;
        const end = start + pageSize;
        const newPosts = data.slice(start, end);

        if (newPosts.length < pageSize) {
            allLoaded.value = true;
        }

        if (reset) {
            posts.value = newPosts;
        } else {
            posts.value = posts.value.concat(newPosts);
        }

        loading.value = false; // Set loading false

        if (!reset && newPosts.length > 0) {
            await nextTick(); // Wait for DOM to update
            // Scroll up slightly to prevent immediate re-trigger
            window.scrollBy(0, -1);
        }
    } catch (e) {
        error.value = 'Failed to fetch posts.';
        loading.value = false;
    }
    // No finally block for loading.value
};

const handleParseFeeds = async () => {
    try {
        error.value = '';
        loading.value = true; // Set loading before operation
        const result = await parseFeeds();
        // fetchPosts(true) will set loading.value to false
        await fetchPosts(true);
        alert(result);
        // await nextTick(); // nextTick is handled within fetchPosts if needed for scrolling - REMOVED
    } catch (e) {
        error.value = 'Failed to parse feeds.';
        loading.value = false; // Ensure loading is reset on error
    }
};

const handleDeleteAllPosts = async () => {
    try {
        error.value = '';
        loading.value = true; // Set loading before operation
        const result = await deleteAllPosts();
        // fetchPosts(true) will set loading.value to false
        await fetchPosts(true);
        alert(result);
        // await nextTick(); - REMOVED
    } catch (e) {
        error.value = 'Failed to delete posts.';
        loading.value = false; // Ensure loading is reset on error
    }
};

const handleScroll = () => {
    if (loading.value || allLoaded.value) return;
    const scrollY = window.scrollY || window.pageYOffset;
    const visible = window.innerHeight;
    const pageHeight = document.documentElement.scrollHeight;
    if (scrollY + visible + 200 >= pageHeight) {
        loading.value = true; // Set loading true to prevent multiple triggers
        page.value++;
        fetchPosts(); // fetchPosts will set loading.value to false
    }
};

onMounted(() => {
    loading.value = true; // Set loading true for initial fetch
    fetchPosts(true);
    window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
});
</script>

<style>
/* Ensure images in post descriptions fit the card width */
.post-img {
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
}

/* Fixed bottom-right loading spinner */
.loading-spinner-fixed {
    position: fixed;
    bottom: 20px;
    right: 20px;
    z-index: 1000; /* Ensure it's above other content */
}
</style>
