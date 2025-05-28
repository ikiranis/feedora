<template>
    <div class="scrollable-post-view" ref="scrollableContainerRef">
        <div class="container py-4">
            <div class="row justify-content-between mb-4 align-items-center">
                <div class="col-auto">
                    <h2>{{ language.get('Posts') }}</h2>
                </div>
                <div class="col-auto d-flex align-items-center">
                    <button @click="handleRefreshPosts" class="btn btn-outline-primary me-3"
                        :title="language.get('Refresh Posts') || 'Refresh Posts'"
                        :disabled="loading">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" 
                            class="bi bi-arrow-clockwise" :class="{ 'spin': loading }" viewBox="0 0 16 16">
                            <title>{{ language.get('Refresh Posts') || 'Refresh Posts' }}</title>
                            <path fill-rule="evenodd" d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z"/>
                            <path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z"/>
                        </svg>
                    </button>
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
                    <button @click="handleDeleteAllPosts" class="btn btn-danger me-3">
                        {{ language.get('Delete All Posts') }}
                    </button>
                    <div class="input-group" style="width: 250px;">
                        <span class="input-group-text">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-search" viewBox="0 0 16 16">
                                <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
                            </svg>
                        </span>
                        <input 
                            v-model="searchTerm"
                            type="text" 
                            class="form-control"
                            :placeholder="language.get('Search posts by title...')"
                            :title="language.get('Filter posts by title')"
                        />
                    </div>
                </div>
            </div>
            <div v-if="posts.length > 0" class="row g-4">
                <div v-for="post in posts" :key="post.id" :data-post-id="post.id"
                    :class="showDetails ? 'col-12 col-md-6 col-lg-4' : 'col-12'">
                    <PostElement :post="post" :show-details="showDetails" @post-read="handlePostRead" />
                </div>
                <div v-if="allLoaded && posts.length > 0 && !searchTerm.trim()" class="text-center w-100 text-muted my-4">
                    {{ language.get('No more posts') || 'No more posts' }}
                </div>
            </div>
            <div v-else-if="!loading && posts.length === 0 && !searchTerm.trim()" class="text-center text-muted mt-5">
                {{ language.get('No posts found') }}
            </div>
            <div v-else-if="!loading && posts.length === 0 && searchTerm.trim()" class="text-center text-muted mt-5">
                {{ language.get('No posts match your search') || 'No posts match your search' }}
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
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { getPosts, parseFeeds, deleteAllPosts, markAsRead } from '@/api/post';
import { getFeedOperationStatus } from '@/api/feed';
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
const intersectionObserver = ref<IntersectionObserver | null>(null);
const observedPosts = ref<Set<string>>(new Set()); // Track which posts are being observed
const lastScrollTop = ref(0); // Track scroll position to determine scroll direction
const searchTerm = ref('');
const searchTimeout = ref<number | null>(null);

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

        const data = await getPosts(page.value, pageSize, searchTerm.value);

        if (data.length < pageSize) {
            allLoaded.value = true;
        }

        if (reset) {
            posts.value = data;
        } else {
            posts.value = posts.value.concat(data);
        }

        page.value++; // Increment page for the next fetch

        // Re-observe post elements after posts are loaded
        nextTick(() => {
            observePostElements();
        });
    } catch (e) {
        if (
            typeof e === 'object' &&
            e !== null &&
            'response' in e &&
            typeof (e as any).response === 'object' &&
            (e as any).response !== null &&
            'data' in (e as any).response &&
            typeof (e as any).response.data === 'object' &&
            (e as any).response.data !== null &&
            'message' in (e as any).response.data &&
            'status' in (e as any).response.data &&
            typeof (e as any).response.data.status === 'number'
        ) {
            errorStore.set(true, (e as any).response.data.message, (e as any).response.data.status);
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
        // Check if a feed operation is already running
        const statusResponse = await getFeedOperationStatus();
        
        if (statusResponse.isRunning) {
            errorStore.set(true, language.get('Cannot parse feeds: Another feed operation is currently in progress. Please try again in a few moments.') || 'Cannot parse feeds: Another feed operation is currently in progress. Please try again in a few moments.', 409);
            return;
        }
    } catch (e: any) {
        console.warn('Could not check feed operation status:', e);
    }

    try {
        loading.value = true;
        const result = await parseFeeds();
        await fetchPosts(true); // Refresh posts
        errorStore.set(true, result, 200); // Use errorStore for success
    } catch (e: any) { // Added :any to e for response access
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
 * Handles refreshing the posts by fetching the latest data.
 */
const handleRefreshPosts = async () => {
    loading.value = true;
    // Clean up current intersection observer
    cleanupIntersectionObserver();
    await fetchPosts(true); // Reset and fetch fresh posts
    // Re-setup intersection observer after posts are loaded
    nextTick(() => {
        setupIntersectionObserver();
    });
};

/**
 * Handles when a post is marked as read.
 * Updates the local state to reflect the change immediately.
 */
const handlePostRead = (postId: string) => {
    const postIndex = posts.value.findIndex(post => post.id === postId);
    if (postIndex !== -1) {
        posts.value[postIndex].read = true;
        // Remove from observed posts since it's now read
        observedPosts.value.delete(postId);
        
        // Stop observing this post element since it's now read
        if (intersectionObserver.value && scrollableContainerRef.value) {
            const postElement = scrollableContainerRef.value.querySelector(`[data-post-id="${postId}"]`);
            if (postElement) {
                intersectionObserver.value.unobserve(postElement);
            }
        }
    }
};

/**
 * Marks a post as read via API call.
 * Used when posts are scrolled past.
 */
const markPostAsReadAPI = async (postId: string) => {
    try {
        await markAsRead(postId);
        handlePostRead(postId); // Update local state
    } catch (error) {
        console.error('Failed to mark post as read:', error);
    }
};

/**
 * Sets up intersection observer to track when posts are scrolled past.
 * Posts that completely disappear from the viewport are marked as read only when scrolling down.
 */
const setupIntersectionObserver = () => {
    if (!scrollableContainerRef.value) return;

    intersectionObserver.value = new IntersectionObserver(
        (entries) => {
            // Get current scroll position to determine direction
            const currentScrollTop = scrollableContainerRef.value?.scrollTop || 0;
            const isScrollingDown = currentScrollTop > lastScrollTop.value;
            lastScrollTop.value = currentScrollTop;

            entries.forEach((entry) => {
                const postElement = entry.target as HTMLElement;
                const postId = postElement.getAttribute('data-post-id');
                
                if (!postId) return;

                const post = posts.value.find(p => p.id === postId);
                if (!post || post.read) return;

                if (entry.isIntersecting) {
                    // Post is visible - track it
                    observedPosts.value.add(postId);
                } else {
                    // Post is no longer visible - mark as read only if scrolling down and it was previously observed
                    if (observedPosts.value.has(postId) && isScrollingDown) {
                        observedPosts.value.delete(postId);
                        markPostAsReadAPI(postId);
                    }
                }
            });
        },
        {
            root: scrollableContainerRef.value,
            rootMargin: '0px',
            threshold: 0 // Trigger when post is completely out of view
        }
    );

    // Observe all current post elements
    observePostElements();
};

/**
 * Observes all unread post elements for intersection changes.
 */
const observePostElements = () => {
    if (!intersectionObserver.value || !scrollableContainerRef.value) return;

    const postElements = scrollableContainerRef.value.querySelectorAll('[data-post-id]');
    postElements.forEach((element) => {
        const postId = element.getAttribute('data-post-id');
        if (postId) {
            const post = posts.value.find(p => p.id === postId);
            // Only observe unread posts
            if (post && !post.read) {
                intersectionObserver.value!.observe(element);
            }
        }
    });
};

/**
 * Cleans up intersection observer.
 */
const cleanupIntersectionObserver = () => {
    if (intersectionObserver.value) {
        intersectionObserver.value.disconnect();
        intersectionObserver.value = null;
    }
    observedPosts.value.clear();
    lastScrollTop.value = 0;
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

// Watch for search term changes with debouncing
watch(searchTerm, (_) => {
    // Clear existing timeout
    if (searchTimeout.value) {
        clearTimeout(searchTimeout.value);
    }
    
    // Set new timeout for debounced search
    searchTimeout.value = setTimeout(() => {
        // Cleanup intersection observer before search
        cleanupIntersectionObserver();
        
        // Reset and fetch with search
        loading.value = true;
        fetchPosts(true);
        
        // Re-setup intersection observer after search
        nextTick(() => {
            setupIntersectionObserver();
        });
    }, 500); // 500ms debounce delay
});

// Lifecycle hooks
onMounted(() => {
    loading.value = true;
    fetchPosts(true); // Initial fetch
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.addEventListener('scroll', handleScroll);
        // Initialize scroll position
        lastScrollTop.value = scrollableContainerRef.value.scrollTop;
    }
    // Setup intersection observer for marking posts as read when scrolled past
    nextTick(() => {
        setupIntersectionObserver();
    });
    // Ensure body overflow is hidden when this component is active
    document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.removeEventListener('scroll', handleScroll);
    }
    // Cleanup search timeout
    if (searchTimeout.value) {
        clearTimeout(searchTimeout.value);
    }
    // Cleanup intersection observer
    cleanupIntersectionObserver();
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

/* Refresh button spin animation */
.spin {
    animation: spin 1s linear infinite;
}

@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}
</style>
