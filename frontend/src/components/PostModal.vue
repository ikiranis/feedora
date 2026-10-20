<template>
    <Teleport to="body">
        <div class="post-modal-backdrop" @click.self="emit('close')">
            <div ref="bodyRef" class="post-modal" :class="{ zoomed }" role="dialog" aria-modal="true" :aria-label="post.title"
                @touchstart="onTouchStart" @touchend="onTouchEnd" @touchcancel="touchStart = null">
                <button type="button" class="post-modal-close" :aria-label="language.get('Close') || 'Close'"
                    :title="language.get('Close') || 'Close'" @click="emit('close')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
                    </svg>
                </button>

                <!-- The slide animation only plays after a swipe, so switching posts with the keyboard isn't animated -->
                <article :key="post.id" class="post-modal-article" :class="swipeDirection && `slide-from-${swipeDirection}`"
                    @animationend.self="swipeDirection = null">
                    <!-- Tweets have no real title, their text is the content -->
                    <header class="post-modal-header" :class="{ 'is-tweet': isTweet }">
                        <LiveBadge :post="post" class="title-live-badge" />
                        <a v-if="!isTweet" :href="post.link" target="_blank" class="post-modal-title">{{ post.title }}</a>
                        <div class="post-modal-meta">
                            <img v-if="siteIcon" :src="siteIcon" class="post-modal-icon" :style="iconStyle" alt="" @error="iconFailed = true" />
                            <span class="text-truncate feed-link" role="button" :title="language.get('Show the posts of this feed') || 'Show the posts of this feed'"
                                @click.stop="post.feed && emit('show-feed', post.feed.id)">{{ post.feed?.title }}</span>
                            <span v-if="post.author" class="text-truncate">&middot; {{ post.author }}</span>
                            <span v-if="post.pubDate" class="flex-shrink-0">&middot; {{ new Date(post.pubDate).toLocaleString() }}</span>
                        </div>
                        <div class="post-modal-actions">
                            <button type="button" class="post-modal-action" :class="{ favorite: post.favorite }" @click="emit('toggle-favorite', post.id)"
                                :title="post.favorite ? (language.get('Remove from favorites') || 'Remove from favorites') : (language.get('Add to favorites') || 'Add to favorites')">
                                <svg v-if="post.favorite" xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                                    <path d="M3.612 15.443c-.386.198-.824-.149-.746-.592l.83-4.73L.173 6.765c-.329-.314-.158-.888.283-.95l4.898-.696L7.538.792c.197-.39.73-.39.927 0l2.184 4.327 4.898.696c.441.062.612.636.282.95l-3.522 3.356.83 4.73c.078.443-.36.79-.746.592L8 13.187l-4.389 2.256z"/>
                                </svg>
                                <svg v-else xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                                    <path d="M2.866 14.85c-.078.444.36.791.746.593l4.39-2.256 4.389 2.256c.386.198.824-.149.746-.592l-.83-4.73 3.522-3.356c.33-.314.16-.888-.282-.95l-4.898-.696L8.465.792a.513.513 0 0 0-.927 0L5.354 5.12l-4.898.696c-.441.062-.612.636-.283.95l3.523 3.356-.83 4.73zm4.905-2.767-3.686 1.894.694-3.957a.56.56 0 0 0-.163-.505L1.71 6.745l4.052-.576a.53.53 0 0 0 .393-.288L8 2.223l1.847 3.658a.53.53 0 0 0 .393.288l4.052.575-2.906 2.77a.56.56 0 0 0-.163.506l.694 3.957-3.686-1.894a.5.5 0 0 0-.461 0z"/>
                                </svg>
                            </button>
                            <button type="button" class="post-modal-action" :class="{ saved: post.saved }" @click="emit('toggle-saved', post.id)"
                                :title="post.saved ? (language.get('Remove from saved') || 'Remove from saved') : (language.get('Save for later') || 'Save for later')">
                                <svg v-if="post.saved" xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                                    <path d="M2 2v13.5a.5.5 0 0 0 .74.439L8 13.069l5.26 2.87A.5.5 0 0 0 14 15.5V2a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z"/>
                                </svg>
                                <svg v-else xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                                    <path d="M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v13.5a.5.5 0 0 1-.777.416L8 13.101l-5.223 2.815A.5.5 0 0 1 2 15.5V2zm2-1a1 1 0 0 0-1 1v12.566l4.723-2.482a.5.5 0 0 1 .554 0L13 14.566V2a1 1 0 0 0-1-1H4z"/>
                                </svg>
                            </button>
                            <a :href="post.link" target="_blank" class="post-modal-action" :title="language.get('Open original') || 'Open original'">
                                <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                                    <path fill-rule="evenodd" d="M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5z"/>
                                    <path fill-rule="evenodd" d="M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0v-5z"/>
                                </svg>
                            </a>
                            <PostShareActions :post="post" />
                            <button v-if="logCount" type="button" class="post-modal-action logs-action" @click="emit('show-logs', post)"
                                :title="language.get('The content of this post couldn\'t be fully retrieved') + ' — ' + language.get('View logs')">
                                <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                                    <path d="M7.938 2.016A.13.13 0 0 1 8.002 2a.13.13 0 0 1 .063.016.15.15 0 0 1 .054.057l6.857 11.667c.036.06.035.124.002.183a.2.2 0 0 1-.054.06.1.1 0 0 1-.066.017H1.146a.1.1 0 0 1-.066-.017.2.2 0 0 1-.054-.06.18.18 0 0 1 .002-.183L7.884 2.073a.15.15 0 0 1 .054-.057m1.044-.45a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767z"/>
                                    <path d="M7.002 12a1 1 0 1 1 2 0 1 1 0 0 1-2 0M7.1 5.995a.905.905 0 1 1 1.8 0l-.35 3.507a.552.552 0 0 1-1.1 0z"/>
                                </svg>
                            </button>
                        </div>
                    </header>

                    <div v-if="youtubeEmbedUrl && embeddable" class="ratio ratio-16x9 mb-4">
                        <iframe :src="youtubeEmbedUrl" :title="post.title" allowfullscreen referrerpolicy="strict-origin-when-cross-origin"
                            @load="youtubeEmbedStore.watchPlayer($event.target as HTMLIFrameElement, videoId!)"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe>
                    </div>
                    <!-- Thumbnail while checking the video, or a link to YouTube when it can't be embedded -->
                    <a v-else-if="videoId" :href="post.link" target="_blank" class="ratio ratio-16x9 mb-4 video-thumbnail"
                        :title="language.get('Watch on YouTube') || 'Watch on YouTube'">
                        <img :src="`https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`" alt="" />
                        <span v-if="embeddable === false" class="video-thumbnail-overlay">
                            <span class="video-thumbnail-play">
                                <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 16 16" fill="currentColor">
                                    <path d="M11.596 8.697l-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>
                                </svg>
                            </span>
                            {{ language.get('Watch on YouTube') || 'Watch on YouTube' }}
                        </span>
                        <VideoDuration :seconds="post.duration" />
                    </a>
                    <div v-if="contentLoading" class="post-modal-loading">
                        <span class="spinner-border spinner-border-sm" role="status"></span>
                        {{ language.get('Loading full article...') || 'Loading full article...' }}
                    </div>
                    <div v-else-if="contentHtml" ref="contentRef" class="post-modal-content" :class="{ 'tweet-content': isTweet }" v-html="contentHtml"
                        @error.capture="hideBrokenImage"></div>
                    <LinkPreview v-if="linkPreview" :preview="linkPreview" large />
                </article>
            </div>
            <!-- Shown for a moment after a swipe, before moving to the previous/next post -->
            <Transition name="swipe-arrow">
                <div v-if="swipeArrow" class="swipe-arrow" :class="`swipe-arrow-${swipeArrow}`" aria-hidden="true">
                    <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" fill="currentColor" viewBox="0 0 16 16">
                        <path v-if="swipeArrow === 'left'" fill-rule="evenodd" d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0"/>
                        <path v-else fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708"/>
                    </svg>
                </div>
            </Transition>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { postContentStore } from '@/functions/postContentStore';
import { hideBrokenImage } from '@/functions/hideBrokenImage';
import { youtubeEmbedStore } from '@/functions/youtubeEmbedStore';
import LiveBadge from '@/components/LiveBadge.vue';
import VideoDuration from '@/components/VideoDuration.vue';
import PostShareActions from '@/components/PostShareActions.vue';
import LinkPreview from '@/components/LinkPreview.vue';
import { isTweetLink, removePreviewedLink, tweetLinkPreview } from '@/functions/tweet';
import { muteVideos, playFirstVideo } from '@/functions/videoPlayback';
import { language } from '@/functions/languageStore';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps<{
    post: PostType;
}>();

const emit = defineEmits<{
    'close': []
    'toggle-saved': [postId: string]
    'toggle-favorite': [postId: string]
    'show-logs': [post: PostType]
    'show-feed': [feedId: string]
    'next': []
    'previous': []
}>();

const bodyRef = ref<HTMLElement | null>(null);
const iconFailed = ref(false);

const siteIcon = computed(() => {
    if (iconFailed.value) return null;
    try {
        const hostname = new URL(props.post.feed?.htmlUrl || props.post.link).hostname;
        return `https://www.google.com/s2/favicons?domain=${hostname}&sz=32`;
    } catch {
        return null;
    }
});

// Custom background color behind the favicon, set in the feed's settings
const iconStyle = computed(() => props.post.feed?.iconBackgroundColor
    ? { backgroundColor: props.post.feed.iconBackgroundColor, padding: '2px' }
    : undefined);

/**
 * YouTube video id if the post links to a video (watch, youtu.be, shorts, embed or live links).
 */
const videoId = computed(() => {
    try {
        const url = new URL(props.post.link);
        const host = url.hostname.replace(/^(www\.|m\.)/, '');
        let videoId: string | null = null;
        if (host === 'youtu.be') {
            videoId = url.pathname.slice(1).split('/')[0];
        } else if (host === 'youtube.com' || host === 'youtube-nocookie.com') {
            videoId = url.pathname.match(/^\/(?:shorts|embed|live)\/([\w-]+)/)?.[1] ?? url.searchParams.get('v');
        }
        return videoId && /^[\w-]{11}$/.test(videoId) ? videoId : null;
    } catch {
        return null;
    }
});

const youtubeEmbedUrl = computed(() => videoId.value ? youtubeEmbedStore.embedUrl(videoId.value) : null);
// Whether the video can be played embedded (undefined until checked); some uploaders disable embedding
const embeddable = computed(() => videoId.value ? youtubeEmbedStore.isEmbeddable(videoId.value) : undefined);

const fullContent = computed(() => postContentStore.get(props.post.id) ?? null);

const isTweet = computed(() => isTweetLink(props.post.link));
// Tweets: preview of the link in the text, which is removed from the shown text
const linkPreview = computed(() => tweetLinkPreview(props.post));
const contentHtml = computed(() => {
    const html = fullContent.value || props.post.description;
    return html && linkPreview.value ? removePreviewedLink(html, linkPreview.value.image) : html;
});
const contentLoading = computed(() => postContentStore.isLoading(props.post.id));

// Tweets: their video plays muted when the post is opened, once its content is shown
const contentRef = ref<HTMLElement | null>(null);
watch([contentHtml, contentLoading], async () => {
    if (!isTweet.value || contentLoading.value) return;
    await nextTick();
    muteVideos(contentRef.value);
    playFirstVideo(contentRef.value);
}, { immediate: true });
// Log entries of the post, e.g. when its page couldn't be read
const logCount = computed(() => postContentStore.logCount(props.post.id));

// Load the full article (videos show the player instead) and start at the top when switching posts
watch(() => props.post.id, () => {
    iconFailed.value = false;
    bodyRef.value?.scrollTo({ top: 0 });
    if (videoId.value) youtubeEmbedStore.check(videoId.value);
    else postContentStore.load(props.post.id);
}, { immediate: true });

// Swipe left/right on touch screens to go to the next/previous post
const SWIPE_MIN_DISTANCE = 70; // px the finger must move horizontally
const SWIPE_MAX_SLOPE = 0.6; // Vertical movement allowed per horizontal px, so scrolling the article isn't taken as a swipe
const touchStart = ref<{ x: number, y: number } | null>(null);
// Side the shown post slides in from after a swipe
const swipeDirection = ref<'left' | 'right' | null>(null);

// While zoomed in, horizontal panning must stay possible, so the pan-y touch-action is dropped
const zoomed = ref(false);
const updateZoomed = () => { zoomed.value = !!window.visualViewport && window.visualViewport.scale > 1.01; };
onMounted(() => {
    updateZoomed();
    window.visualViewport?.addEventListener('resize', updateZoomed);
});
onBeforeUnmount(() => {
    window.visualViewport?.removeEventListener('resize', updateZoomed);
    clearTimeout(swipeArrowTimer);
});

// How long the arrow is shown after a swipe before moving to the previous/next post
const SWIPE_ARROW_DURATION = 250;
// Arrow pointing to the side of the post being moved to, null when hidden
const swipeArrow = ref<'left' | 'right' | null>(null);
let swipeArrowTimer: ReturnType<typeof setTimeout> | undefined;

/**
 * Returns true when the element or one of its parents (inside the modal) scrolls horizontally,
 * e.g. a wide table or code block, where a horizontal swipe should scroll it instead.
 */
const inHorizontalScroller = (element: Element | null): boolean => {
    while (element && element !== bodyRef.value) {
        if (element.scrollWidth > element.clientWidth && ['auto', 'scroll'].includes(getComputedStyle(element).overflowX)) {
            return true;
        }
        element = element.parentElement;
    }
    return false;
};

const onTouchStart = (event: TouchEvent) => {
    // Multi-touch is a pinch zoom, not a swipe
    if (event.touches.length !== 1 || inHorizontalScroller(event.target as Element)) {
        touchStart.value = null;
        return;
    }
    touchStart.value = { x: event.touches[0].clientX, y: event.touches[0].clientY };
};

const onTouchEnd = (event: TouchEvent) => {
    const start = touchStart.value;
    touchStart.value = null;
    // Ignore swipes while the page is zoomed in, where they pan the page, and while a previous swipe is still pending
    if (!start || swipeArrow.value || (window.visualViewport && window.visualViewport.scale > 1.01)) return;

    const touch = event.changedTouches[0];
    const dx = touch.clientX - start.x;
    const dy = touch.clientY - start.y;
    if (Math.abs(dx) < SWIPE_MIN_DISTANCE || Math.abs(dy) > Math.abs(dx) * SWIPE_MAX_SLOPE) return;

    // Swiping left brings the next post in from the right
    const side = dx < 0 ? 'right' : 'left';
    swipeArrow.value = side;
    swipeArrowTimer = setTimeout(() => {
        swipeArrow.value = null;
        swipeDirection.value = side;
        if (side === 'right') emit('next');
        else emit('previous');
    }, SWIPE_ARROW_DURATION);
};

/**
 * Scrolls the modal body, used by the keyboard handler of the posts view.
 */
const scrollBy = (top: number) => bodyRef.value?.scrollBy({ top });

defineExpose({ scrollBy });
</script>

<style scoped>
/* Full-page reader: no card, border or dividers, just the article on the page background */
.post-modal-backdrop {
    position: fixed;
    inset: 0;
    z-index: 1055;
    background-color: var(--bg-color);
}

[data-bs-theme="dark"] .post-modal-backdrop {
    background-color: #16181d;
}

.post-modal {
    height: 100%;
    overflow-y: auto;
    overflow-x: hidden;
    overscroll-behavior: contain;
    /* Horizontal finger moves are swipes between posts, so the browser must not pan the page sideways during them */
    touch-action: pan-y pinch-zoom;
}

.post-modal.zoomed {
    touch-action: auto;
}

/* The post shown after a swipe slides in from the side it was swiped from */
.slide-from-right {
    animation: slide-from-right 0.22s ease-out;
}

.slide-from-left {
    animation: slide-from-left 0.22s ease-out;
}

@keyframes slide-from-right {
    from { transform: translateX(40px); opacity: 0; }
    to { transform: none; opacity: 1; }
}

@keyframes slide-from-left {
    from { transform: translateX(-40px); opacity: 0; }
    to { transform: none; opacity: 1; }
}

.post-modal-close {
    position: fixed;
    top: 1.25rem;
    right: 1.75rem;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 3rem;
    height: 3rem;
    border: 0;
    border-radius: 50%;
    background-color: rgba(127, 127, 127, 0.15);
    color: var(--text-color);
    transition: background-color 0.2s;
}

/* Full-height bar on the side of the post being moved to */
.swipe-arrow {
    position: fixed;
    top: 0;
    bottom: 0;
    z-index: 2;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 4rem;
    background-color: rgba(127, 127, 127, 0.12);
    color: var(--text-color);
    pointer-events: none;
}

.swipe-arrow-left {
    left: 0;
}

.swipe-arrow-right {
    right: 0;
}

.swipe-arrow-enter-active {
    transition: opacity 0.12s ease-out, transform 0.12s ease-out;
}

.swipe-arrow-leave-active {
    transition: opacity 0.1s ease-in;
}

.swipe-arrow-enter-from {
    opacity: 0;
}

.swipe-arrow-left.swipe-arrow-enter-from {
    transform: translateX(-100%);
}

.swipe-arrow-right.swipe-arrow-enter-from {
    transform: translateX(100%);
}

.swipe-arrow-leave-to {
    opacity: 0;
}

.post-modal-close:hover {
    background-color: rgba(127, 127, 127, 0.3);
}

.post-modal-article {
    max-width: 860px;
    margin: 0 auto;
    /* The title starts on the same row as the close button */
    padding: 1.25rem 1.5rem 5rem;
}

.post-modal-header {
    margin-bottom: 2rem;
}

.post-modal-title {
    display: block;
    font-size: 2.6rem;
    font-weight: 800;
    line-height: 1.2;
    letter-spacing: -0.01em;
    color: var(--text-color);
    text-decoration: none;
}

/* Keeps the title clear of the close button when the article reaches the screen edge */
@media (max-width: 1024px) {
    .post-modal-title {
        padding-right: 3.5rem;
    }
}

.title-live-badge {
    margin-bottom: 0.75rem;
    font-size: 0.85rem;
}

.post-modal-title:hover {
    color: #4d8dff;
}

.post-modal-meta {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-top: 1.25rem;
    color: #8b919c;
    font-size: 1.05rem;
    min-width: 0;
}

.post-modal-header.is-tweet .post-modal-meta {
    margin-top: 0;
}

.feed-link {
    cursor: pointer;
}

.feed-link:hover {
    color: #4d8dff;
    text-decoration: underline;
}

.post-modal-icon {
    width: 20px;
    height: 20px;
    flex-shrink: 0;
    border-radius: 50%;
}

.post-modal-actions {
    display: flex;
    align-items: center;
    gap: 1.25rem;
    margin-top: 1.5rem;
}

.post-modal-action {
    display: flex;
    padding: 0.25rem;
    border: 0;
    background: transparent;
    color: #8b919c;
    transition: color 0.2s;
}

.post-modal-action:hover {
    color: var(--text-color);
}

.post-modal-action.logs-action,
.post-modal-action.logs-action:hover {
    color: #e0a800;
}

.post-modal-action.saved {
    color: #4d8dff;
}

.post-modal-action.favorite {
    color: #f5b301;
}

.ratio iframe {
    border: 0;
    border-radius: 0.5rem;
}

.video-thumbnail {
    overflow: hidden;
    border-radius: 0.5rem;
    background-color: #000;
}

.video-thumbnail img {
    object-fit: cover;
}

.video-thumbnail-overlay {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.75rem;
    background-color: rgba(0, 0, 0, 0.35);
    color: #fff;
    font-size: 1.1rem;
    font-weight: 600;
    text-decoration: none;
}

.video-thumbnail-play {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 4rem;
    height: 4rem;
    border-radius: 50%;
    background-color: rgba(0, 0, 0, 0.6);
    transition: background-color 0.2s;
}

.video-thumbnail:hover .video-thumbnail-play {
    background-color: #ff0033;
}

.post-modal-loading {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: #8b919c;
    font-size: 1.1rem;
}

@media (max-width: 768px) {
    /* Centers the first title line on the close button */
    .post-modal-article {
        padding-top: 1.6rem;
    }

    .post-modal-title {
        font-size: 1.9rem;
    }
}

/* Full article content: readable line length and paragraph spacing */
.post-modal-content {
    font-family: Georgia, "Iowan Old Style", "Times New Roman", serif;
    font-size: 1.35rem;
    line-height: 1.65;
    word-break: break-word;
}

/* Tweets read like on Twitter, in the interface font */
.post-modal-content.tweet-content {
    font-family: inherit;
    font-size: 1.3rem;
    line-height: 1.5;
}

.post-modal-content :deep(img),
.post-modal-content :deep(video),
.post-modal-content :deep(iframe) {
    display: block;
    max-width: 100% !important;
    max-height: 500px !important;
    width: auto !important;
    height: auto !important;
    object-fit: contain;
    border-radius: 0.25rem;
    margin: 1rem 0;
}

.post-modal-content :deep(p) {
    margin: 0 0 1em;
}

.post-modal-content :deep(h1),
.post-modal-content :deep(h2),
.post-modal-content :deep(h3),
.post-modal-content :deep(h4) {
    margin: 1.4em 0 0.6em;
    font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
    font-size: 1.6rem;
    font-weight: 700;
}

.post-modal-content :deep(ul),
.post-modal-content :deep(ol) {
    margin: 0 0 1em;
    padding-left: 1.5em;
}

.post-modal-content :deep(blockquote) {
    margin: 0 0 1em;
    color: #8b919c;
    font-style: italic;
}

.post-modal-content :deep(figure) {
    margin: 0 0 1em;
}

.post-modal-content :deep(a) {
    color: #4d8dff;
}

.post-modal-content :deep(figcaption) {
    font-size: 0.95rem;
    color: #8b919c;
}

/* Tweets: their photos side by side, cropped to the same height */
.post-modal-content :deep(.tweet-photos) {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr));
    gap: 0.5rem;
    margin: 1rem 0;
}

.post-modal-content :deep(.tweet-photos img) {
    width: 100% !important;
    height: 100% !important;
    max-height: 500px !important;
    margin: 0;
    object-fit: cover;
    border-radius: 0.75rem;
}

.post-modal-content :deep(.tweet-photos img:only-child) {
    object-fit: contain;
    width: auto !important;
    max-width: 100% !important;
}
</style>
