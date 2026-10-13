<template>
    <div class="d-flex posts-layout">
        <aside class="folder-sidebar d-none d-md-block">
            <nav class="folder-sidebar-inner py-4">
                <button type="button" class="folder-item" :class="{ active: !selectedFolderId && !selectedFeedId }"
                    @click="selectAll" :title="language.get('All Posts') || 'All Posts'">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-collection flex-shrink-0" viewBox="0 0 16 16">
                        <path d="M2.5 3.5a.5.5 0 0 1 0-1h11a.5.5 0 0 1 0 1h-11zm2-2a.5.5 0 0 1 0-1h7a.5.5 0 0 1 0 1h-7zM0 13a1.5 1.5 0 0 0 1.5 1.5h13A1.5 1.5 0 0 0 16 13V6a1.5 1.5 0 0 0-1.5-1.5h-13A1.5 1.5 0 0 0 0 6v7zm1.5.5A.5.5 0 0 1 1 13V6a.5.5 0 0 1 .5-.5h13a.5.5 0 0 1 .5.5v7a.5.5 0 0 1-.5.5h-13z"/>
                    </svg>
                    <span class="folder-name">{{ language.get('All Posts') || 'All Posts' }}</span>
                    <span class="folder-count">{{ unreadCounts.total }}</span>
                </button>

                <hr class="my-2" />

                <template v-for="folder in visibleFolders" :key="folder.id">
                    <div class="folder-item" :class="{ active: selectedFolderId === folder.id }"
                        role="button" @click="selectFolder(folder.id)" :title="folder.name">
                        <button type="button" class="folder-toggle" @click.stop="toggleFolder(folder.id)"
                            :title="expandedFolders.has(folder.id) ? language.get('Collapse') : language.get('Expand')">
                            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="currentColor"
                                class="bi bi-chevron-right folder-chevron" :class="{ expanded: expandedFolders.has(folder.id) }"
                                viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/>
                            </svg>
                        </button>
                        <span class="folder-name">{{ folder.name }}</span>
                        <span class="folder-count">{{ unreadCounts.folders[folder.id] || 0 }}</span>
                    </div>

                    <template v-if="expandedFolders.has(folder.id)">
                        <button v-for="feed in visibleFeedsByFolder[folder.id] || []" :key="feed.id" type="button"
                            class="folder-item feed-item" :class="{ active: selectedFeedId === feed.id }"
                            @click="selectFeed(feed.id)" :title="feed.title">
                            <img v-if="getFeedIcon(feed)" :src="getFeedIcon(feed)!" class="feed-icon flex-shrink-0"
                                alt="" loading="lazy" @error="failedFeedIcons.add(feed.id)" />
                            <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                class="bi bi-rss flex-shrink-0" viewBox="0 0 16 16">
                                <path d="M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h12zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2H2z"/>
                                <path d="M5.5 12a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm-3-8.5a1 1 0 0 1 1-1c5.523 0 10 4.477 10 10a1 1 0 1 1-2 0 8 8 0 0 0-8-8 1 1 0 0 1-1-1zm0 4a1 1 0 0 1 1-1 6 6 0 0 1 6 6 1 1 0 1 1-2 0 4 4 0 0 0-4-4 1 1 0 0 1-1-1z"/>
                            </svg>
                            <span class="folder-name">{{ feed.title }}</span>
                            <span class="folder-count">{{ feed.count }}</span>
                        </button>
                    </template>
                </template>
            </nav>
        </aside>

        <div class="flex-grow-1 posts-main">
            <div class="container py-4">
                <div class="row justify-content-end mb-4 align-items-center">
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
                                :placeholder="language.get('Search posts by title or description...')"
                                :title="language.get('Filter posts by title or description')"
                            />
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="scrollable-post-view" ref="scrollableContainerRef">
                <div class="container py-4">
                    <div v-if="posts.length > 0" class="row" :class="showDetails ? 'g-4' : 'g-0'">
                        <div v-for="(post, index) in posts" :key="post.id" :data-post-id="post.id"
                            :class="showDetails ? 'col-12 col-md-6 col-lg-4' : 'col-12'"
                            @click="selectedIndex = index">
                            <PostElement :post="post" :show-details="showDetails" :selected="index === selectedIndex"
                                @post-read="handlePostRead" />
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
        </div>
    </div>

    <Error class="error-fixed-bottom" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { getPosts, parseFeeds, deleteAllPosts, markAsRead, getUnreadCounts } from '@/api/post';
import { getFeedOperationStatus } from '@/api/feed';
import { getFolders } from '@/api/folder';
import { PostType, FolderType, UnreadCountsType, FeedUnreadCountType } from '@/types';
import { language } from '@/functions/languageStore';
import PostElement from '@/components/PostElement.vue';
import Error from '@/components/error/Error.vue'; // Changed from ErrorDisplay
import { errorStore } from '@/components/error/errorStore'; // Import errorStore

const posts = ref<PostType[]>([]);
const page = ref(1);
const pageSize = 50;
const loading = ref(false);
const allLoaded = ref(false);
const showDetails = ref(true);
const scrollableContainerRef = ref<HTMLElement | null>(null); // Ref for the scrollable container
const intersectionObserver = ref<IntersectionObserver | null>(null);
const observedPosts = ref<Set<string>>(new Set()); // Track which posts are being observed
const lastScrollTop = ref(0); // Track scroll position to determine scroll direction
const searchTerm = ref('');
const searchTimeout = ref<number | null>(null);
const selectedIndex = ref(-1); // Index of the card selected via keyboard navigation
const folders = ref<FolderType[]>([]);
const unreadCounts = ref<UnreadCountsType>({ total: 0, folders: {}, feeds: [] });
const selectedFolderId = ref<string | null>(null); // null shows the posts of all folders
const selectedFeedId = ref<string | null>(null); // null shows the posts of all feeds
const expandedFolders = ref<Set<string>>(new Set());
const failedFeedIcons = ref<Set<string>>(new Set());

// Folders with unread posts, sorted by name. The selected folder (or the folder of the selected feed)
// stays visible after its last post is read.
const visibleFolders = computed(() => {
    const selectedFeedFolderId = unreadCounts.value.feeds.find(f => f.id === selectedFeedId.value)?.folderId;
    return folders.value
        .filter(folder => (unreadCounts.value.folders[folder.id] || 0) > 0
            || folder.id === selectedFolderId.value
            || folder.id === selectedFeedFolderId)
        .sort((a, b) => a.name.localeCompare(b.name));
});

// Feeds with unread posts grouped by folder id and sorted by title. The selected feed stays visible after its last post is read.
const visibleFeedsByFolder = computed(() => {
    const groups: Record<string, FeedUnreadCountType[]> = {};
    for (const feed of unreadCounts.value.feeds) {
        if (!feed.folderId || (feed.count <= 0 && feed.id !== selectedFeedId.value)) continue;
        (groups[feed.folderId] ||= []).push(feed);
    }
    Object.values(groups).forEach(feeds => feeds.sort((a, b) => a.title.localeCompare(b.title)));
    return groups;
});

/**
 * Favicon URL of the feed's website, or null if it has none or it failed to load.
 */
const getFeedIcon = (feed: FeedUnreadCountType): string | null => {
    if (!feed.htmlUrl || failedFeedIcons.value.has(feed.id)) return null;
    try {
        return `https://www.google.com/s2/favicons?domain=${new URL(feed.htmlUrl).hostname}&sz=32`;
    } catch {
        return null;
    }
};

/**
 * Expands or collapses the feed list of a folder in the sidebar.
 */
const toggleFolder = (folderId: string) => {
    if (expandedFolders.value.has(folderId)) {
        expandedFolders.value.delete(folderId);
    } else {
        expandedFolders.value.add(folderId);
    }
};

/**
 * Fetches the user's folders for the sidebar.
 */
const fetchFolders = async () => {
    try {
        folders.value = await getFolders();
    } catch (e) {
        console.error('Error fetching folders:', e);
        folders.value = [];
    }
};

/**
 * Fetches the unread post counts shown next to each folder in the sidebar.
 */
const fetchUnreadCounts = async () => {
    try {
        unreadCounts.value = await getUnreadCounts();
    } catch (e) {
        console.error('Error fetching unread counts:', e);
    }
};

/**
 * Changes the folder/feed filter of the posts list and reloads it.
 */
const applyFilter = (folderId: string | null, feedId: string | null) => {
    if (selectedFolderId.value === folderId && selectedFeedId.value === feedId) return;
    selectedFolderId.value = folderId;
    selectedFeedId.value = feedId;
    scrollableContainerRef.value?.scrollTo({ top: 0 });
    handleRefreshPosts();
};

const selectAll = () => applyFilter(null, null);
const selectFolder = (folderId: string) => applyFilter(folderId, null);
const selectFeed = (feedId: string) => applyFilter(null, feedId);

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
            selectedIndex.value = -1;
        }

        const data = await getPosts(page.value, pageSize, searchTerm.value, selectedFolderId.value, selectedFeedId.value);

        if (data.length < pageSize) {
            allLoaded.value = true;
        }

        if (reset) {
            posts.value = data;
        } else {
            posts.value = posts.value.concat(data);
        }

        page.value++; // Increment page for the next fetch

        // Re-observe post elements after posts are loaded, and keep loading while the list
        // is too short to scroll, since infinite scroll only triggers on scroll events
        nextTick(() => {
            observePostElements();
            handleScroll();
        });
    } catch (e: any) {
        console.error('Error fetching posts:', e);
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data.message || e.response.data, e.response.status || 500);
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
        fetchUnreadCounts();
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
        fetchUnreadCounts();
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
    fetchUnreadCounts();
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
        const post = posts.value[postIndex];
        if (!post.read) {
            decrementUnreadCount(post);
        }
        post.read = true;
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
 * Lowers the sidebar unread counts after a post has been marked as read.
 */
const decrementUnreadCount = (post: PostType) => {
    unreadCounts.value.total = Math.max(0, unreadCounts.value.total - 1);
    const folderId = post.feed?.folderId;
    if (folderId && unreadCounts.value.folders[folderId]) {
        unreadCounts.value.folders[folderId]--;
    }
    const feed = unreadCounts.value.feeds.find(f => f.id === post.feed?.id);
    if (feed && feed.count > 0) {
        feed.count--;
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
        fetchPosts(); // fetchPosts advances the page itself
    }
};

/**
 * Returns the number of cards per row by counting the cards rendered on the same line as the first one.
 */
const getColumnCount = (): number => {
    if (!scrollableContainerRef.value) return 1;
    const elements = Array.from(scrollableContainerRef.value.querySelectorAll<HTMLElement>('[data-post-id]'));
    if (elements.length === 0) return 1;
    const firstTop = elements[0].offsetTop;
    return Math.max(1, elements.filter(el => el.offsetTop === firstTop).length);
};

/**
 * Selects the card at the given index, scrolls it into view and marks it as read.
 */
const selectPost = (index: number) => {
    if (index < 0 || index >= posts.value.length) return;
    selectedIndex.value = index;

    const post = posts.value[index];
    nextTick(() => {
        const element = scrollableContainerRef.value?.querySelector(`[data-post-id="${post.id}"]`);
        element?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    });

    if (!post.read) {
        markPostAsReadAPI(post.id);
    }
};

/**
 * Opens a link in a background tab. Uses the Feedora Companion extension (see /extension) when installed,
 * otherwise simulates a Cmd/Ctrl+click, which browsers may open as a regular new tab instead.
 */
const openInBackgroundTab = (url: string) => {
    if (document.documentElement.dataset.feedoraExtension) {
        window.postMessage({ type: 'FEEDORA_OPEN_BACKGROUND_TAB', url }, window.location.origin);
        return;
    }

    const isMac = /Mac|iPhone|iPad/.test(navigator.platform);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.target = '_blank';
    anchor.rel = 'noopener noreferrer';
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    anchor.dispatchEvent(new MouseEvent('click', {
        bubbles: true,
        cancelable: true,
        view: window,
        ctrlKey: !isMac,
        metaKey: isMac,
    }));
    anchor.remove();
};

/**
 * Handles arrow key navigation between cards.
 * Enter opens the selected post's link in a new tab, B opens it in a background tab.
 */
const handleKeydown = (event: KeyboardEvent) => {
    const target = event.target as HTMLElement | null;
    if (target && (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))) return;
    if (event.altKey || event.ctrlKey || event.metaKey || posts.value.length === 0) return;

    const current = selectedIndex.value;
    const columns = getColumnCount();
    let next: number;

    switch (event.key) {
        case 'ArrowRight':
            next = current < 0 ? 0 : current + 1;
            break;
        case 'ArrowLeft':
            next = current < 0 ? 0 : current - 1;
            break;
        case 'ArrowDown':
            next = current < 0 ? 0 : Math.min(current + columns, posts.value.length - 1);
            break;
        case 'ArrowUp':
            next = current < 0 ? 0 : current - columns;
            break;
        case 'Enter':
            if (current >= 0 && posts.value[current]?.link) {
                window.open(posts.value[current].link, '_blank');
                event.preventDefault();
            }
            return;
        case 'b':
        case 'B':
            if (current >= 0 && posts.value[current]?.link) {
                openInBackgroundTab(posts.value[current].link);
                event.preventDefault();
            }
            return;
        default:
            return;
    }

    event.preventDefault();
    selectPost(next);
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
    // Clear any existing errors when navigating to posts page
    errorStore.clear();
    
    loading.value = true;
    fetchFolders();
    fetchUnreadCounts();
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
    window.addEventListener('keydown', handleKeydown);
    // Ensure body overflow is hidden when this component is active
    document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.removeEventListener('scroll', handleScroll);
    }
    window.removeEventListener('keydown', handleKeydown);
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
    height: calc(90vh - 120px); /* Adjust height to account for fixed header */
    overflow-y: auto;
}

/* Folder sidebar on the left of the posts list */
.posts-main {
    min-width: 0;
}

.folder-sidebar {
    position: relative;
    width: 240px;
    flex-shrink: 0;
    border-right: 1px solid var(--bs-border-color);
}

/* Absolutely positioned so the sidebar takes the height of the posts column and scrolls on its own */
.folder-sidebar-inner {
    position: absolute;
    inset: 0;
    overflow-y: auto;
    padding-right: 0.75rem;
}

.folder-item {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    width: 100%;
    padding: 0.5rem 0.75rem;
    margin-bottom: 2px;
    border: 0;
    border-radius: 0.5rem;
    background: transparent;
    color: var(--bs-body-color);
    text-align: left;
    cursor: pointer;
}

.folder-item:hover {
    background-color: var(--bs-tertiary-bg);
}

.folder-item.active {
    background-color: var(--bs-secondary-bg);
    color: var(--bs-primary);
    font-weight: 600;
}

/* Chevron that expands a folder to show its feeds */
.folder-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    flex-shrink: 0;
    padding: 0;
    border: 0;
    border-radius: 0.25rem;
    background: transparent;
    color: inherit;
}

.folder-toggle:hover {
    background-color: var(--bs-secondary-bg);
}

.folder-chevron {
    transition: transform 0.15s ease;
}

.folder-chevron.expanded {
    transform: rotate(90deg);
}

.feed-item {
    padding-left: 2.25rem;
    font-size: 0.95em;
}

.feed-icon {
    width: 16px;
    height: 16px;
    border-radius: 3px;
}

.folder-name {
    flex-grow: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.folder-count {
    font-size: 0.85em;
    color: var(--bs-secondary-color);
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
