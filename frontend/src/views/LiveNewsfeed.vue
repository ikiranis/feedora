<template>
    <div class="container py-3">
        <div class="live-column live-toolbar d-flex align-items-center flex-wrap gap-3">
            <h4 class="m-0 d-flex align-items-center gap-2">
                <span class="live-indicator" :class="{ paused: paused || scrolledDown }"></span>
                {{ language.get('Live Newsfeed') || 'Live Newsfeed' }}
            </h4>

            <div class="ms-auto d-flex align-items-center flex-wrap gap-3">
                <span v-if="queue.length > 0" class="badge rounded-pill text-bg-primary"
                    :title="language.get('Posts waiting to be shown') || 'Posts waiting to be shown'">
                    {{ queue.length }} {{ queue.length === 1 ? (language.get('new post') || 'new post') : (language.get('new posts') || 'new posts') }}
                </span>
                <span v-if="checkFailed" class="text-danger small"
                    :title="language.get('The last check for new posts failed') || 'The last check for new posts failed'">
                    {{ language.get('Offline') || 'Offline' }}
                </span>
                <button type="button" class="btn btn-sm btn-outline-secondary d-flex align-items-center gap-1" @click="paused = !paused"
                    :title="paused ? (language.get('Resume the live newsfeed') || 'Resume the live newsfeed') : (language.get('Pause the live newsfeed') || 'Pause the live newsfeed')">
                    <svg v-if="paused" xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
                        <path d="m11.596 8.697-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393"/>
                    </svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M5.5 3.5A1.5 1.5 0 0 1 7 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5m5 0A1.5 1.5 0 0 1 12 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5"/>
                    </svg>
                    {{ paused ? (language.get('Resume') || 'Resume') : (language.get('Pause') || 'Pause') }}
                </button>
            </div>
        </div>
    </div>

    <div class="live-view-wrapper">
    <button v-if="scrolledDown" type="button" class="btn btn-primary btn-sm shadow back-to-top d-flex align-items-center gap-1" @click="scrollToTop"
        :title="language.get('Back to top and resume new posts') || 'Back to top and resume new posts'">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
            <path fill-rule="evenodd" d="M8 15a.5.5 0 0 0 .5-.5V2.707l3.146 3.147a.5.5 0 0 0 .708-.708l-4-4a.5.5 0 0 0-.708 0l-4 4a.5.5 0 1 0 .708.708L7.5 2.707V14.5a.5.5 0 0 0 .5.5"/>
        </svg>
        <span v-if="queue.length > 0" class="badge rounded-pill text-bg-light">{{ queue.length }}</span>
    </button>
    <div class="scrollable-live-view" ref="scrollableContainerRef" @scroll="handleScroll">
        <div class="container pt-1 pb-4">
            <div class="live-column">
                <TransitionGroup :name="animate ? 'live-post' : ''" tag="div" class="d-flex flex-column gap-4">
                    <div v-for="post in posts" :key="post.id" class="live-item" :class="{ fresh: freshPostIds.has(post.id) }">
                        <PostElement :post="post" :show-details="true" :autoplay="post.id === currentPostId"
                            @post-read="handlePostRead" @toggle-read="toggleRead" @toggle-saved="toggleSaved" @toggle-favorite="toggleFavorite"
                            @show-logs="logsPost = $event" @show-feed="showFeed" @open="modalPostId = $event" />
                    </div>
                </TransitionGroup>

                <div v-if="!loading && posts.length === 0" class="text-center text-muted mt-5">
                    {{ language.get('No unread posts. New posts will show up here.') || 'No unread posts. New posts will show up here.' }}
                </div>
                <div v-if="loading" class="text-center mt-5">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            </div>
        </div>
    </div>

    </div>

    <PostModal v-if="modalPost" :post="modalPost"
        @close="modalPostId = null" @toggle-saved="toggleSaved" @toggle-favorite="toggleFavorite" @show-logs="logsPost = $event" @show-feed="showFeed" />

    <LogsModal :post-id="logsPost?.id" :feed-id="logsPost?.feed?.id" :title="logsPost?.title" :show="!!logsPost"
        @logs-cleared="logsPost && postContentStore.clearLogCount(logsPost.id)" @modal-hidden="logsPost = null" />

    <Error />
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getLivePosts, getLiveRefreshSeconds, getPosts, markAsRead, markAsUnread, setPostSaved, setPostFavorite } from '@/api/post';
import { PostType } from '@/types';
import { language } from '@/functions/languageStore';
import { postContentStore } from '@/functions/postContentStore';
import PostElement from '@/components/PostElement.vue';
import PostModal from '@/components/PostModal.vue';
import LogsModal from '@/components/log/LogsModal.vue';
import Error from '@/components/error/Error.vue';
import { errorStore } from '@/components/error/errorStore';

const INITIAL_POSTS = 30; // Latest posts shown when the page opens
const MAX_POSTS = 200; // The oldest posts are dropped beyond this, so a page left open doesn't grow forever
// New posts are shown one at a time, spread over the time until the next check: at most this far apart,
// closer when many are waiting, so they're all shown before the next check brings more
const MAX_REVEAL_INTERVAL_MS = 5000;
const MIN_REVEAL_INTERVAL_MS = 300;
const FRESH_MS = 4000; // How long a newly shown post stays highlighted
const SCROLL_PAUSE_PX = 100; // Scrolled further down than this, new posts wait until the list is scrolled back to the top

const router = useRouter();
const posts = ref<PostType[]>([]); // Shown posts, newest first
const queue = ref<PostType[]>([]); // New posts waiting to be shown, the next one first
const seenPostIds = new Set<string>(); // Shown or queued posts, since the checks overlap
const freshPostIds = ref<Set<string>>(new Set());
// Posts slide in only when the top of the list is shown, since the slide would fight keeping the scroll position
const animate = ref(true);
// Server time to check for new posts from
let listedAt: string | null = null;
const refreshSeconds = ref(60); // From the settings
const secondsLeft = ref(60); // Until the next check
const paused = ref(false);
// Scrolled down to read a post: new posts are still checked for, but only shown once back at the top
const scrolledDown = ref(false);
const loading = ref(false);
const checking = ref(false);
const checkFailed = ref(false);
const scrollableContainerRef = ref<HTMLElement | null>(null);
const modalPostId = ref<string | null>(null);
const modalPost = computed(() => posts.value.find(p => p.id === modalPostId.value) ?? null);
const logsPost = ref<PostType | null>(null);
// The newest post, whose tweet video plays while the top of the list is shown and no post is opened in the modal
const currentPostId = computed(() => scrolledDown.value || modalPostId.value ? null : posts.value[0]?.id ?? null);
let tickTimer: number | null = null;
let revealTimer: number | null = null;
let unmounted = false;

/**
 * Loads the refresh interval and the latest unread posts.
 */
const loadInitialPosts = async () => {
    loading.value = true;
    try {
        refreshSeconds.value = await getLiveRefreshSeconds().catch(() => refreshSeconds.value);
        secondsLeft.value = refreshSeconds.value;
        const result = await getPosts(1, INITIAL_POSTS, undefined, null, null, false, false);
        posts.value = result.posts;
        posts.value.forEach(p => seenPostIds.add(p.id));
        listedAt = result.listedAt;
    } catch (e: any) {
        console.error('Failed to load the posts:', e);
        errorStore.set(true, e.response?.data?.message || language.get('Failed to load posts') || 'Failed to load posts.', e.response?.status || 500);
    } finally {
        loading.value = false;
    }
};

/**
 * Queues the unread posts added since the last check, oldest published first so the newest ends up on top.
 * Posts read while the page is open stay shown, so the list doesn't change under the reader.
 * The refresh interval is read again too, so a changed setting applies without reloading the page.
 */
const checkForNewPosts = async () => {
    if (checking.value || !listedAt) return;
    checking.value = true;
    try {
        const [result, seconds] = await Promise.all([
            getLivePosts(listedAt),
            getLiveRefreshSeconds().catch(() => refreshSeconds.value),
        ]);
        const newPosts = result.posts.filter(p => !p.read && !seenPostIds.has(p.id));
        newPosts.forEach(p => seenPostIds.add(p.id));
        newPosts.sort((a, b) => publishedTime(a) - publishedTime(b));
        queue.value.push(...newPosts);
        scheduleReveal(0);
        listedAt = result.listedAt ?? listedAt;
        refreshSeconds.value = seconds;
        checkFailed.value = false;
    } catch (e) {
        // Shown in the toolbar instead of the error alert, since it's retried at the next check
        console.error('Failed to check for new posts:', e);
        checkFailed.value = true;
    } finally {
        checking.value = false;
        secondsLeft.value = refreshSeconds.value;
    }
};

// Posts without a publish date count as just published
const publishedTime = (post: PostType) => post.pubDate ? new Date(post.pubDate).getTime() : Date.now();

/**
 * Counts down to the next check, every second.
 */
const tick = () => {
    if (paused.value || checking.value) return;
    secondsLeft.value--;
    if (secondsLeft.value <= 0) {
        checkForNewPosts();
    }
};

/**
 * Time until the next post is shown: the time left until the next check, shared by the waiting posts.
 */
const revealDelay = () => Math.min(MAX_REVEAL_INTERVAL_MS,
    Math.max(MIN_REVEAL_INTERVAL_MS, secondsLeft.value * 1000 / Math.max(1, queue.value.length)));

/**
 * Shows the queued posts one at a time, until the queue is empty. Does nothing if they're already being shown.
 */
const scheduleReveal = (delay: number) => {
    if (revealTimer || unmounted || queue.value.length === 0) return;
    revealTimer = window.setTimeout(async () => {
        revealTimer = null;
        if (paused.value || scrolledDown.value) {
            scheduleReveal(1000);
            return;
        }
        await revealNextPost();
        scheduleReveal(revealDelay());
    }, delay);
};

/**
 * Shows the next queued post on top. When the list is scrolled down, the scroll position is kept,
 * so the post being read doesn't move.
 */
const revealNextPost = async () => {
    const post = queue.value.shift();
    if (!post) return;
    const container = scrollableContainerRef.value;
    const scrolledDown = !!container && container.scrollTop > 0;
    const previousHeight = container?.scrollHeight ?? 0;

    animate.value = !scrolledDown;
    posts.value.unshift(post);
    if (posts.value.length > MAX_POSTS) {
        posts.value.splice(MAX_POSTS).forEach(p => seenPostIds.delete(p.id));
    }

    freshPostIds.value = new Set(freshPostIds.value).add(post.id);
    setTimeout(() => {
        const ids = new Set(freshPostIds.value);
        ids.delete(post.id);
        freshPostIds.value = ids;
    }, FRESH_MS);

    if (scrolledDown && container) {
        await nextTick();
        container.scrollTop += container.scrollHeight - previousHeight;
    }
};

const handleScroll = () => {
    scrolledDown.value = (scrollableContainerRef.value?.scrollTop ?? 0) > SCROLL_PAUSE_PX;
};

/**
 * Scrolls back to the top, where the waiting posts are shown again.
 */
const scrollToTop = () => {
    scrollableContainerRef.value?.scrollTo({ top: 0, behavior: 'smooth' });
};

const handlePostRead = (postId: string) => {
    const post = posts.value.find(p => p.id === postId);
    if (post) post.read = true;
};

const toggleRead = async (postId: string) => {
    const post = posts.value.find(p => p.id === postId);
    if (!post) return;
    try {
        if (post.read) {
            await markAsUnread(postId);
        } else {
            await markAsRead(postId);
        }
        post.read = !post.read;
    } catch (e: any) {
        console.error('Failed to change the read state of the post:', e);
        errorStore.set(true, post.read ? (language.get('Failed to mark post as unread') || 'Failed to mark post as unread.')
            : (language.get('Failed to mark post as read') || 'Failed to mark post as read.'), e.response?.status || 500);
    }
};

const toggleSaved = async (postId: string) => {
    const post = posts.value.find(p => p.id === postId);
    if (!post) return;
    try {
        await setPostSaved(postId, !post.saved);
        post.saved = !post.saved;
    } catch (e: any) {
        console.error('Failed to save post:', e);
        errorStore.set(true, e.response?.data?.message || language.get('Failed to save post') || 'Failed to save post.', e.response?.status || 500);
    }
};

const toggleFavorite = async (postId: string) => {
    const post = posts.value.find(p => p.id === postId);
    if (!post) return;
    try {
        await setPostFavorite(postId, !post.favorite);
        post.favorite = !post.favorite;
    } catch (e: any) {
        console.error('Failed to change the favorite state of the post:', e);
        errorStore.set(true, e.response?.data?.message || language.get('Failed to update favorites') || 'Failed to update favorites.', e.response?.status || 500);
    }
};

/**
 * Opens the posts page with the posts of the feed.
 */
const showFeed = (feedId: string) => {
    router.push({ name: 'Posts', query: { feed: feedId } });
};

const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && modalPostId.value) {
        modalPostId.value = null;
    }
};

onMounted(async () => {
    errorStore.clear();
    window.addEventListener('keydown', handleKeydown);
    await loadInitialPosts();
    tickTimer = window.setInterval(tick, 1000);
});

onUnmounted(() => {
    unmounted = true;
    window.removeEventListener('keydown', handleKeydown);
    if (tickTimer) clearInterval(tickTimer);
    if (revealTimer) clearTimeout(revealTimer);
});
</script>

<style scoped>
.live-column {
    max-width: 760px;
    margin: 0 auto;
}

.live-view-wrapper {
    position: relative;
}

.back-to-top {
    position: absolute;
    top: 0.75rem;
    right: 1.5rem;
    z-index: 10;
}

.scrollable-live-view {
    height: calc(100vh - 110px); /* Viewport minus top bar and toolbar */
    overflow-y: auto;
}

/* On mobile the sidebar is a top bar above the page */
@media (max-width: 991.98px) {
    .scrollable-live-view {
        height: calc(100dvh - 130px);
    }
}

/* Pulsing red dot while live, grey while paused */
.live-indicator {
    display: inline-block;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background-color: #dc3545;
    animation: live-pulse 1.6s ease-out infinite;
}

.live-indicator.paused {
    background-color: #6c757d;
    animation: none;
}

@keyframes live-pulse {
    0% { box-shadow: 0 0 0 0 rgba(220, 53, 69, 0.6); }
    70% { box-shadow: 0 0 0 10px rgba(220, 53, 69, 0); }
    100% { box-shadow: 0 0 0 0 rgba(220, 53, 69, 0); }
}

/* A newly shown post slides in and is highlighted for a while */
.live-item {
    border-radius: 1rem;
    transition: box-shadow 1s ease;
}

.live-item.fresh {
    box-shadow: 0 0 0 3px rgba(13, 110, 253, 0.5);
}

.live-post-enter-active {
    transition: opacity 0.6s ease, transform 0.6s ease;
}

.live-post-enter-from {
    opacity: 0;
    transform: translateY(-20px);
}

.live-post-move {
    transition: transform 0.6s ease;
}
</style>
