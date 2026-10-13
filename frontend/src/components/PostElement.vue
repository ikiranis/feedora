<template>
    <div v-if="showDetails" class="custom-card h-100" :class="{ 'unread-post': !post.read, 'read-post': post.read, 'selected-post': selected }" @click="handleCardClick">
        <!-- Media: playing video, YouTube thumbnail or first image of the post -->
        <div v-if="playing && youtubeEmbedUrl" class="card-media" @click.stop>
            <iframe :src="`${youtubeEmbedUrl}?autoplay=1`" :title="post.title" allowfullscreen
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe>
        </div>
        <div v-else-if="thumbnailUrl" class="card-media" :class="{ 'is-short': youtubeVideo?.isShort }"
            @click="youtubeVideo && (playing = true)">
            <img v-if="youtubeVideo?.isShort" :src="thumbnailUrl" class="media-backdrop" alt="" aria-hidden="true" />
            <img :src="thumbnailUrl" class="media-image" alt="" loading="lazy" @error="thumbnailFailed = true" />
            <span v-if="youtubeVideo" class="media-badge">
                <svg v-if="youtubeVideo.isShort" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M17.77 10.32l-1.2-.5L18 9.06a3.74 3.74 0 0 0-3.5-6.62L6 6.94a3.74 3.74 0 0 0 .23 6.74l1.2.49L6 14.93a3.75 3.75 0 0 0 3.5 6.63l8.5-4.5a3.74 3.74 0 0 0-.23-6.74zM10 14.65V9.35L15 12z"/>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 0 0 .5 6.2 31.4 31.4 0 0 0 0 12a31.4 31.4 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 0 0 2.1-2.1A31.4 31.4 0 0 0 24 12a31.4 31.4 0 0 0-.5-5.8zM9.6 15.6V8.4l6.2 3.6z"/>
                </svg>
                {{ youtubeVideo.isShort ? 'Shorts' : 'YouTube' }}
            </span>
            <span v-if="youtubeVideo" class="media-play">
                <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M11.596 8.697l-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>
                </svg>
            </span>
        </div>

        <div class="card-content">
            <a :href="post.link" target="_blank" class="post-title" @click.stop>{{ post.title }}</a>
            <div class="post-source">
                <img v-if="siteIcon" :src="siteIcon" class="site-icon source-icon" alt="" loading="lazy" @error="iconFailed = true" />
                <span class="text-truncate">{{ post.feed?.title }}</span>
                <span v-if="post.feed?.folderName" class="post-folder text-truncate">&middot; {{ post.feed.folderName }}</span>
            </div>
            <p v-if="!youtubeVideo && excerpt" class="post-excerpt" :class="{ 'no-media': !thumbnailUrl }">{{ excerpt }}</p>

            <div class="card-footer-row">
                <span class="post-time" :title="post.pubDate ? new Date(post.pubDate).toLocaleString() : ''">{{ shortDate }}</span>
                <div class="d-flex align-items-center gap-3">
                    <a :href="post.link" target="_blank" class="footer-action" :title="language.get('Open original') || 'Open original'" @click.stop>
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5z"/>
                            <path fill-rule="evenodd" d="M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0v-5z"/>
                        </svg>
                    </a>
                    <span class="read-indicator" :class="{ unread: !post.read }"
                        :title="post.read ? (language.get('Read') || 'Read') : (language.get('Unread') || 'Unread')"></span>
                </div>
            </div>
        </div>
    </div>
    <div v-else class="list-row" :class="{ 'unread-post': !post.read, 'read-post': post.read, 'selected-post': selected }" @click="handleCardClick">
        <div class="list-line">
            <span class="read-dot" :class="{ unread: !post.read }"
                :title="post.read ? (language.get('Read') || 'Read') : (language.get('Unread') || 'Unread')"></span>
            <div class="list-source" :title="post.feed?.folderName ? `${post.feed?.title} — ${post.feed.folderName}` : post.feed?.title">
                <img v-if="siteIcon" :src="siteIcon" class="site-icon list-source-icon" alt="" loading="lazy" @error="iconFailed = true" />
                <span class="text-truncate">{{ post.feed?.title }}</span>
            </div>
            <div class="list-main">
                <span v-if="youtubeVideo" class="list-video-badge">
                    <svg v-if="youtubeVideo.isShort" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M17.77 10.32l-1.2-.5L18 9.06a3.74 3.74 0 0 0-3.5-6.62L6 6.94a3.74 3.74 0 0 0 .23 6.74l1.2.49L6 14.93a3.75 3.75 0 0 0 3.5 6.63l8.5-4.5a3.74 3.74 0 0 0-.23-6.74zM10 14.65V9.35L15 12z"/>
                    </svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 0 0 .5 6.2 31.4 31.4 0 0 0 0 12a31.4 31.4 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 0 0 2.1-2.1A31.4 31.4 0 0 0 24 12a31.4 31.4 0 0 0-.5-5.8zM9.6 15.6V8.4l6.2 3.6z"/>
                    </svg>
                </span>
                <a :href="post.link" target="_blank" class="list-title" @click.stop>{{ post.title }}</a>
                <span v-if="excerpt" class="list-excerpt">{{ excerpt }}</span>
            </div>
            <span class="list-time" :title="post.pubDate ? new Date(post.pubDate).toLocaleString() : ''">{{ shortDate }}</span>
        </div>
        <!-- Expanded details for the selected post -->
        <div v-if="selected" class="list-details" @click.stop>
            <div v-if="youtubeEmbedUrl" class="ratio ratio-16x9 video-embed video-embed-summary">
                <iframe :src="youtubeEmbedUrl" :title="post.title" allowfullscreen
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe>
            </div>
            <div v-if="contentLoading" class="content-loading">
                <span class="spinner-border spinner-border-sm" role="status"></span>
                {{ language.get('Loading full article...') || 'Loading full article...' }}
            </div>
            <!-- While the full content loads, show only the spinner so the description isn't swapped out (no flicker) -->
            <div v-else-if="fullContent || post.description" class="summary-description"
                :class="{ 'post-content': fullContent }" v-html="fullContent || post.description"></div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { markAsRead } from '@/api/post';
import { postContentStore } from '@/functions/postContentStore';
import { language } from '@/functions/languageStore';
import { computed, ref, watch } from 'vue';

const props = defineProps<{
    post: PostType;
    showDetails: boolean;
    selected?: boolean;
}>();

const iconFailed = ref(false);

/**
 * Favicon URL of the post's site, based on the feed's website (or the post link as fallback).
 */
const siteIcon = computed(() => {
    if (iconFailed.value) return null;
    try {
        const hostname = new URL(props.post.feed?.htmlUrl || props.post.link).hostname;
        return `https://www.google.com/s2/favicons?domain=${hostname}&sz=32`;
    } catch {
        return null;
    }
});

const thumbnailFailed = ref(false);
const playing = ref(false); // Grid card: YouTube thumbnail was clicked and the player is shown

/**
 * YouTube video info if the post links to a video (watch, youtu.be, shorts, embed or live links).
 */
const youtubeVideo = computed(() => {
    try {
        const url = new URL(props.post.link);
        const host = url.hostname.replace(/^(www\.|m\.)/, '');
        let videoId: string | null = null;
        let isShort = false;

        if (host === 'youtu.be') {
            videoId = url.pathname.slice(1).split('/')[0];
        } else if (host === 'youtube.com' || host === 'youtube-nocookie.com') {
            const match = url.pathname.match(/^\/(shorts|embed|live)\/([\w-]+)/);
            videoId = match ? match[2] : url.searchParams.get('v');
            isShort = match?.[1] === 'shorts';
        }

        return videoId && /^[\w-]{11}$/.test(videoId) ? { id: videoId, isShort } : null;
    } catch {
        return null;
    }
});

const youtubeEmbedUrl = computed(() =>
    youtubeVideo.value ? `https://www.youtube-nocookie.com/embed/${youtubeVideo.value.id}` : null
);

/**
 * Parsed post description (DOMParser doesn't load images or run scripts).
 */
const descriptionDoc = computed(() =>
    props.post.description ? new DOMParser().parseFromString(props.post.description, 'text/html') : null
);

/**
 * Thumbnail for the card: the YouTube thumbnail for videos, otherwise the first image in the description.
 */
const thumbnailUrl = computed(() => {
    if (thumbnailFailed.value) return null;
    if (youtubeVideo.value) return `https://i.ytimg.com/vi/${youtubeVideo.value.id}/hqdefault.jpg`;

    const src = descriptionDoc.value?.querySelector('img[src]')?.getAttribute('src');
    return src && /^https?:\/\//.test(src) ? src : null;
});

/**
 * Plain-text excerpt of the description.
 */
const excerpt = computed(() => descriptionDoc.value?.body.textContent?.replace(/\s+/g, ' ').trim() || '');

/**
 * Publication time for today's posts, otherwise the date.
 */
const shortDate = computed(() => {
    if (!props.post.pubDate) return '';
    const date = new Date(props.post.pubDate);
    return date.toDateString() === new Date().toDateString()
        ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : date.toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' });
});

watch(() => props.post.id, () => {
    iconFailed.value = false;
    thumbnailFailed.value = false;
    playing.value = false;
});

// Full article HTML from the shared cache (preloaded by the posts list), shown when the post is opened in list mode
const fullContent = computed(() => postContentStore.get(props.post.id) ?? null);
const contentLoading = computed(() => postContentStore.isLoading(props.post.id));
const isOpened = computed(() => !!props.selected && !props.showDetails && !youtubeVideo.value);

/**
 * Loads the full article content when the post is opened (expanded) in list mode, if it wasn't preloaded.
 */
watch([isOpened, () => props.post.id], ([opened]) => {
    if (opened) postContentStore.load(props.post.id);
}, { immediate: true });

const emit = defineEmits<{
    'post-read': [postId: string]
}>();

const handleCardClick = async () => {
    // Only mark as read if it's currently unread
    if (!props.post.read) {
        try {
            await markAsRead(props.post.id);
            // Emit event to notify parent component that the post has been marked as read
            emit('post-read', props.post.id);
        } catch (error) {
            console.error('Failed to mark post as read:', error);
        }
    }
};
</script>

<style scoped>
.custom-card {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--border-color);
    border-radius: 1rem;
    background-color: var(--card-bg);
    overflow: hidden;
    transition: transform 0.2s, box-shadow 0.2s, border-color 0.3s;
    cursor: pointer;
}

[data-bs-theme="dark"] .custom-card {
    background-color: #16181d;
    border-color: #2a2e36;
}

.custom-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
}

/* Media area */
.card-media {
    position: relative;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    background-color: #000;
    flex-shrink: 0;
}

.card-media iframe {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    border: 0;
}

.media-image {
    position: relative;
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

/* Shorts: vertical frame centered over a blurred copy of itself */
.card-media.is-short .media-image {
    width: auto;
    margin: 0 auto;
    aspect-ratio: 9 / 16;
}

.media-backdrop {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    filter: blur(18px) brightness(0.5);
    transform: scale(1.2);
}

.media-badge {
    position: absolute;
    top: 0.75rem;
    left: 0.75rem;
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.2rem 0.55rem;
    border-radius: 0.4rem;
    background-color: rgba(0, 0, 0, 0.75);
    color: #fff;
    font-size: 0.8rem;
    font-weight: 600;
}

.media-play {
    position: absolute;
    top: 50%;
    left: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 3.25rem;
    height: 3.25rem;
    border-radius: 50%;
    background-color: rgba(0, 0, 0, 0.6);
    color: #fff;
    transform: translate(-50%, -50%);
    opacity: 0;
    transition: opacity 0.2s;
}

.card-media:hover .media-play {
    opacity: 1;
}

/* Text area */
.card-content {
    display: flex;
    flex-direction: column;
    flex-grow: 1;
    padding: 1.25rem 1.5rem 1rem;
}

.post-title {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 1.15rem;
    font-weight: 700;
    line-height: 1.35;
    color: var(--text-color);
    text-decoration: none;
    transition: color 0.2s;
}

.post-title:hover {
    color: #4d8dff;
}

.read-post .post-title {
    opacity: 0.6;
}

.read-post .media-image {
    opacity: 0.75;
}

.post-source {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-top: 0.6rem;
    color: #8b919c;
    font-size: 0.95rem;
    min-width: 0;
}

.source-icon {
    width: 20px;
    height: 20px;
    border-radius: 50%;
}

.post-folder {
    flex-shrink: 1;
    opacity: 0.8;
}

.post-excerpt {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin: 0.75rem 0 0;
    color: #8b919c;
    font-size: 0.92rem;
    line-height: 1.45;
}

.post-excerpt.no-media {
    -webkit-line-clamp: 5;
}

.card-footer-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    padding-top: 1.25rem;
    color: #8b919c;
}

.post-time {
    font-size: 0.95rem;
}

.footer-action {
    display: flex;
    color: inherit;
    transition: color 0.2s;
}

.footer-action:hover {
    color: var(--text-color);
}

.read-indicator {
    width: 18px;
    height: 18px;
    border: 2px solid currentColor;
    border-radius: 50%;
}

.read-indicator.unread {
    background: radial-gradient(circle, #4d8dff 0 4px, transparent 5px);
    border-color: #4d8dff;
}

.custom-card.selected-post {
    border-color: #0d6efd !important;
    box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.4);
}

.video-embed {
    border-radius: 0.375rem;
    overflow: hidden;
    cursor: auto;
}

.video-embed iframe {
    border: 0;
}

.video-embed-summary {
    max-width: 640px;
}

.site-icon {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    border-radius: 3px;
}

/* List mode rows */
.list-row {
    border-bottom: 1px solid var(--border-color);
    cursor: pointer;
    transition: background-color 0.15s;
}

[data-bs-theme="dark"] .list-row {
    border-bottom-color: #2a2e36;
}

.list-row:hover {
    background-color: rgba(127, 127, 127, 0.08);
}

.list-row.selected-post {
    background-color: rgba(77, 141, 255, 0.1);
    box-shadow: inset 3px 0 0 #4d8dff;
}

.list-line {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 0.6rem 0.75rem;
    min-width: 0;
}

.read-dot {
    width: 9px;
    height: 9px;
    flex-shrink: 0;
    border-radius: 50%;
}

.read-dot.unread {
    background-color: #4d8dff;
}

.list-source {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    width: 200px;
    flex-shrink: 0;
    color: #8b919c;
    min-width: 0;
}

.list-source-icon {
    border-radius: 50%;
}

.list-main {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
}

.list-video-badge {
    display: inline-flex;
    align-self: center;
    flex-shrink: 0;
    color: var(--text-color);
}

.list-title {
    flex-shrink: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-weight: 700;
    color: var(--text-color);
    text-decoration: none;
}

.list-title:hover {
    color: #4d8dff;
}

.read-post .list-title {
    font-weight: 500;
    opacity: 0.6;
}

.list-excerpt {
    flex: 1 1 0;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    color: #8b919c;
}

.list-time {
    flex-shrink: 0;
    color: #8b919c;
    font-size: 0.95rem;
}

.list-details {
    padding: 0.25rem 1rem 1rem calc(0.75rem + 9px + 1rem);
    cursor: auto;
}

@media (max-width: 768px) {
    .list-source {
        width: auto;
    }

    .list-source span {
        display: none;
    }

    .list-excerpt {
        display: none;
    }
}

.summary-description {
    font-weight: normal;
    word-break: break-word;
    padding-top: 0.75rem;
    border-top: 1px solid var(--border-color);
    overflow-x: hidden;
}

/* Keep media in the expanded description small enough to fit the row */
.summary-description :deep(img),
.summary-description :deep(video),
.summary-description :deep(iframe) {
    display: block;
    max-width: 100% !important;
    max-height: 300px !important;
    width: auto !important;
    height: auto !important;
    object-fit: contain;
    border-radius: 0.25rem;
    margin: 0.5rem 0;
}

.content-loading {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0;
    color: #8b919c;
    font-size: 0.9rem;
}

/* Full article content: readable line length and paragraph spacing */
.post-content {
    max-width: 80ch;
    line-height: 1.7;
    font-size: 1rem;
}

.post-content :deep(p) {
    margin: 0 0 1em;
}

.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3),
.post-content :deep(h4) {
    margin: 1.4em 0 0.6em;
    font-size: 1.15rem;
    font-weight: 600;
}

.post-content :deep(ul),
.post-content :deep(ol) {
    margin: 0 0 1em;
    padding-left: 1.5em;
}

.post-content :deep(blockquote) {
    margin: 0 0 1em;
    padding-left: 1em;
    border-left: 3px solid var(--border-color);
    color: #8b919c;
}

.post-content :deep(figure) {
    margin: 0 0 1em;
}

.post-content :deep(figcaption) {
    font-size: 0.85rem;
    color: #8b919c;
}

.post-content :deep(img) {
    max-height: 450px !important;
}

/* Ensure images in post descriptions fit the card width if details are shown */
:deep(.post-img) {
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
}
</style>
