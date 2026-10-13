<template>
    <div v-if="showDetails" class="card h-100 shadow-sm custom-card" :class="{ 'unread-post': !post.read, 'read-post': post.read, 'selected-post': selected }" @click="handleCardClick">
        <div class="card-body d-flex flex-column">
            <h5 class="card-title d-flex align-items-start gap-2">
                <img v-if="siteIcon" :src="siteIcon" class="site-icon site-icon-card" alt="" loading="lazy" @error="iconFailed = true" />
                <a :href="post.link" target="_blank" class="post-title" @click.stop>{{ post.title }}</a>
            </h5>
            <div class="d-flex align-items-center mb-2 gap-2">
                <span class="badge feed-title-badge p-1">{{ post.feed?.title }}</span>
                <span v-if="post.feed?.folderName" class="badge bg-secondary text-light p-1">{{ post.feed.folderName }}</span>
            </div>
            <div v-if="youtubeEmbedUrl" class="ratio ratio-16x9 mb-2 video-embed" @click.stop>
                <iframe :src="youtubeEmbedUrl" :title="post.title" loading="lazy" allowfullscreen
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe>
            </div>
            <div class="mb-2" v-else-if="post.description">
                <div class="description-content-limiter" v-html="post.description"></div>
            </div>
            <div class="mt-auto">
                <span class="badge bg-light text-dark" v-if="post.pubDate">{{ new Date(post.pubDate).toLocaleString() }}</span>
            </div>
        </div>
    </div>
    <div v-else class="post-summary custom-summary" :class="{ 'unread-post': !post.read, 'read-post': post.read, 'selected-post': selected }" @click="handleCardClick">
        <div class="d-flex w-100 justify-content-between align-items-center">
            <img v-if="siteIcon" :src="siteIcon" class="site-icon me-2" alt="" loading="lazy" @error="iconFailed = true" />
            <a :href="post.link" target="_blank" class="post-title-link me-auto" @click.stop>{{ post.title }}</a>
            <span class="post-date ms-2 text-muted" v-if="post.pubDate">
                {{ new Date(post.pubDate).toLocaleString() }}
            </span>
        </div>
        <div class="summary-meta text-muted mt-1" style="font-size:0.95em;">
            <span v-if="post.feed?.title">{{ post.feed.title }}</span>
            <span v-if="post.feed?.folderName"> &mdash; {{ post.feed.folderName }}</span>
        </div>
        <!-- Expanded details for the selected post -->
        <div v-if="selected && youtubeEmbedUrl" class="ratio ratio-16x9 mt-3 video-embed video-embed-summary" @click.stop>
            <iframe :src="youtubeEmbedUrl" :title="post.title" allowfullscreen
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe>
        </div>
        <div v-if="selected && post.description" class="summary-description mt-3" v-html="post.description"></div>
    </div>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { markAsRead } from '@/api/post';
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

/**
 * Embed URL if the post links to a YouTube video (watch, youtu.be, shorts, embed or live links).
 */
const youtubeEmbedUrl = computed(() => {
    try {
        const url = new URL(props.post.link);
        const host = url.hostname.replace(/^(www\.|m\.)/, '');
        let videoId: string | null = null;

        if (host === 'youtu.be') {
            videoId = url.pathname.slice(1).split('/')[0];
        } else if (host === 'youtube.com' || host === 'youtube-nocookie.com') {
            const match = url.pathname.match(/^\/(?:shorts|embed|live)\/([\w-]+)/);
            videoId = match ? match[1] : url.searchParams.get('v');
        }

        return videoId && /^[\w-]{11}$/.test(videoId)
            ? `https://www.youtube-nocookie.com/embed/${videoId}`
            : null;
    } catch {
        return null;
    }
});

watch(() => props.post.id, () => { iconFailed.value = false; });

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
    border: 1px solid var(--border-color);
    border-radius: 0.5rem;
    background-color: var(--card-bg);
    transition: transform 0.2s, box-shadow 0.2s, background-color 0.3s, border-color 0.3s;
    cursor: pointer;
}

.custom-card.unread-post {
    border: 2px solid #333;
    font-weight: bold;
    background-color: #f8f9fa;
}

[data-bs-theme="dark"] .custom-card.unread-post {
    border: 2px solid #6c757d;
    background-color: #212529;
}

.custom-card.read-post {
    border: 1px solid var(--border-color);
}

[data-bs-theme="dark"] .custom-card.read-post {
    background-color: var(--bg-color);
    border: 1px solid #2d3748;
}

.custom-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

[data-bs-theme="dark"] .custom-card:hover {
    box-shadow: 0 4px 15px rgba(255, 255, 255, 0.1);
}

.custom-card.selected-post,
.custom-summary.selected-post {
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

.site-icon-card {
    width: 20px;
    height: 20px;
    margin-top: 0.2rem;
}

.post-title {
    font-size: 1.25rem;
    font-weight: bold;
    color: var(--text-color);
    text-decoration: none;
    transition: color 0.3s;
}

.post-title:hover {
    color: #007bff;
    text-decoration: underline;
}

.feed-title-badge {
    background-color: transparent !important;
    color: var(--text-color) !important;
    border: 1px solid var(--border-color);
}

.custom-summary {
    padding: 0.75rem;
    border: 1px solid var(--border-color);
    border-radius: 0.5rem;
    background-color: var(--card-bg);
    transition: background-color 0.3s, border-color 0.3s;
    cursor: pointer;
}

.custom-summary.unread-post {
    border: 2px solid #333;
    font-weight: bold;
    background-color: #f8f9fa;
}

[data-bs-theme="dark"] .custom-summary.unread-post {
    border: 2px solid #6c757d;
    background-color: #212529;
}

.custom-summary.read-post {
    border: 1px solid var(--border-color);
}

[data-bs-theme="dark"] .custom-summary.read-post {
    background-color: var(--bg-color);
    border: 1px solid #2d3748;
}

.custom-summary:hover {
    background-color: #e9ecef;
}

[data-bs-theme="dark"] .custom-summary:hover {
    background-color: #495057;
}

.post-summary {
    padding: 0.5rem 0.75rem;
    border: 1px solid var(--border-color);
    border-radius: 0.25rem;
    background-color: var(--card-bg);
    margin-bottom: 0.5rem;
    transition: background-color 0.3s, border-color 0.3s;
    /* Remove flex to allow second line to break naturally */
}

[data-bs-theme="dark"] .post-summary.read-post {
    background-color: var(--bg-color);
}

.post-title-link {
    text-decoration: none;
    color: #000000;
    font-weight: bold;
    transition: color 0.3s;
}

[data-bs-theme="dark"] .post-title-link {
    color: #ffffff;
}

.post-title-link:hover {
    text-decoration: underline;
}

.post-date {
    font-size: 0.85em;
    text-align: right;
    white-space: nowrap;
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

.description-content-limiter {
    max-height: 6em;
    /* Adjust as needed, roughly 3-4 lines depending on line-height */
    overflow: hidden;
    /* The following properties can help with how text breaks and is truncated,
     but true ellipsis on multi-line v-html is complex.
     This primarily "cuts" the content. */
    display: block;
    /* Ensure it behaves as a block for overflow to work reliably */
    word-break: break-word;
    /* Helps break long words to prevent them from overflowing */
}

/* Ensure images in post descriptions fit the card width if details are shown */
:deep(.post-img) {
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
}
</style>
