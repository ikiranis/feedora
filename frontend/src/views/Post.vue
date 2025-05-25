<template>
    <div class="scrollable-post-view" ref="scrollableContainerRef">
        <div class="container py-4">
            <div class="row justify-content-between mb-4 align-items-center">
                <div class="col-auto">
                    <h2>{{ language.get('Posts') }}</h2>
                </div>
                <div class="col-auto d-flex align-items-center">
                    <button @click="showDetails = !showDetails" class="btn btn-outline-secondary me-3"
                        :title="showDetails ? language.get('Show Summary') : language.get('Show Details')">
                        <svg v-if="showDetails" xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                            fill="currentColor" class="bi bi-list" viewBox="0 0 16 16">
                            <title>{{ language.get('Show Summary') }}</title>
                            <path fill-rule="evenodd"
                                d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5z" />
                        </svg>
                        <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                            class="bi bi-card-text" viewBox="0 0 16 16">
                            <title>{{ language.get('Show Details') }}</title>
                            <path
                                d="M14.5 3a.5.5 0 0 1 .5.5v9a.5.5 0 0 1-.5.5h-13a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5h13zm-13-1A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h13a1.5 1.5 0 0 0 1.5-1.5v-9A1.5 1.5 0 0 0 14.5 2h-13z" />
                            <path
                                d="M3 5.5a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9a.5.5 0 0 1-.5-.5zM3 8a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9A.5.5 0 0 1 3 8zm0 2.5a.5.5 0 0 1 .5-.5h6a.5.5 0 0 1 0 1h-6a.5.5 0 0 1-.5-.5z" />
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
            <div v-if="posts.length > 0" class="row g-4">
                <div v-for="post in posts" :key="post.id" :data-post-id="post.id"
                    :class="showDetails ? 'col-12 col-md-6 col-lg-4' : 'col-12'">
                    <PostElement :post="post" :show-details="showDetails" />
                </div>
                <div v-if="allLoaded && posts.length > 0" class="text-center w-100 text-muted my-4">
                    {{ language.get('No more posts') || 'No more posts' }}
                </div>
            </div>
            <div v-else class="text-center text-muted mt-5">
                {{ language.get('No posts found') }}
            </div>

            <!-- Fixed bottom-right loading spinner -->
            <div v-if="loading" class="loading-spinner-fixed">
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
import { getPosts, parseFeeds, deleteAllPosts } from '@/api/post';
import { PostType } from '@/types';
import { language } from '@/functions/languageStore';
import PostElement from '@/components/PostElement.vue';
import Error from '@/components/error/Error.vue'; // Changed from ErrorDisplay
import { errorStore } from '@/components/error/errorStore'; // Import errorStore

const posts = ref<PostType[]>([]);
const page = ref(1);
const pageSize = 15;
const loading = ref(false);
const allLoaded = ref(false);
const showDetails = ref(true);
const scrollableContainerRef = ref<HTMLElement | null>(null); // Ref for the scrollable container

/**
 * Removes size-related attributes (width, height, srcset, sizes) and inline styles for width/height
 * from <img> tags in an HTML string. It also adds a 'post-img' class to each <img> tag for styling.
 * @param {string} html - The HTML string to process.
 * @returns {string} The cleaned HTML string.
 */
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

/**
 * Fetches posts from the API. It supports pagination and resetting the post list.
 * It also processes post descriptions to remove image size attributes.
 * @param {boolean} [reset=false] - If true, clears existing posts and resets pagination.
 */
const fetchPosts = async (reset = false) => {
    try {
        if (reset) {
            posts.value = [];
            page.value = 1;
            allLoaded.value = false;
        }

        const data = await getPosts();
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

    } catch (e: any) { // Added :any to e for response access
        console.error("Error fetching posts:", e);
        if (e.response && e.response.data && e.response.data.message && typeof e.response.data.status === 'number') {
            errorStore.set(true, e.response.data.message, e.response.data.status);
        } else {
            errorStore.set(true, language.get('Failed to fetch posts') || 'Failed to fetch posts.', 500);
        }
    } finally {
        loading.value = false;
    }
};

/**
 * Handles the action of parsing RSS feeds. It calls the parseFeeds API,
 * then refreshes the post list and shows an alert with the result.
 */
const handleParseFeeds = async () => {
    try {
        loading.value = true;
        const result = await parseFeeds();
        await fetchPosts(true); // Refresh posts
        errorStore.set(true, result, 200); // Use errorStore for success
    } catch (e: any) { // Added :any to e for response access
        console.error("Error parsing feeds:", e);
        if (e.response && e.response.data && e.response.data.message && typeof e.response.data.status === 'number') {
            errorStore.set(true, e.response.data.message, e.response.data.status);
        } else {
            errorStore.set(true, language.get('Failed to parse feeds') || 'Failed to parse feeds.', 500);
        }
    } finally {
        loading.value = false;
    }
};

/**
 * Handles the action of deleting all posts. It calls the deleteAllPosts API,
 * then refreshes the post list and shows an alert with the result.
 */
const handleDeleteAllPosts = async () => {
    try {
        loading.value = true;
        const result = await deleteAllPosts();
        await fetchPosts(true); // Refresh posts
        errorStore.set(true, result, 200); // Use errorStore for success
    } catch (e: any) { // Added :any to e for response access
        console.error("Error deleting all posts:", e);
        if (e.response && e.response.data && e.response.data.message && typeof e.response.data.status === 'number') {
            errorStore.set(true, e.response.data.message, e.response.data.status);
        } else {
            errorStore.set(true, language.get('Failed to delete posts') || 'Failed to delete posts.', 500);
        }
    } finally {
        loading.value = false;
    }
};

/**
 * Handles the scroll event on the scrollable container.
 * When the user scrolls near the bottom, it loads more posts if available.
 */
const handleScroll = () => {
    if (loading.value || allLoaded.value || !scrollableContainerRef.value) return;

    const container = scrollableContainerRef.value;
    const scrollTop = container.scrollTop;
    const clientHeight = container.clientHeight;
    const scrollHeight = container.scrollHeight;

    // Load more when 200px from the bottom
    if (scrollTop + clientHeight + 200 >= scrollHeight) {
        loading.value = true;
        page.value++;
        fetchPosts();
    }
};

// Lifecycle hooks
onMounted(() => {
    loading.value = true;
    fetchPosts(true); // Initial fetch
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.addEventListener('scroll', handleScroll);
    }
    // Ensure body overflow is hidden when this component is active
    document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.removeEventListener('scroll', handleScroll);
    }
    // Restore body overflow when this component is destroyed
    document.body.style.overflow = 'auto';
});

</script>

<style>
/* Make the main view scrollable instead of the whole page */
.scrollable-post-view {
    height: 90vh; /* Adjust height as needed */
    overflow-y: auto;
}

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
    z-index: 1000;
    /* Ensure it's above other content */
}

/* Fixed bottom-left error component */
.error-fixed-bottom {
    position: fixed;
    bottom: 5em;
    left: 20px;
    z-index: 1000; /* Same z-index as spinner, or adjust as needed */
}
</style>
