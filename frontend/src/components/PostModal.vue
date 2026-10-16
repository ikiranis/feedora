<template>
    <Teleport to="body">
        <div class="post-modal-backdrop" @click.self="emit('close')">
            <div ref="bodyRef" class="post-modal" role="dialog" aria-modal="true" :aria-label="post.title">
                <button type="button" class="post-modal-close" :aria-label="language.get('Close') || 'Close'"
                    :title="language.get('Close') || 'Close'" @click="emit('close')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
                    </svg>
                </button>

                <article class="post-modal-article">
                    <header class="post-modal-header">
                        <LiveBadge :post="post" class="title-live-badge" />
                        <a :href="post.link" target="_blank" class="post-modal-title">{{ post.title }}</a>
                        <div class="post-modal-meta">
                            <img v-if="siteIcon" :src="siteIcon" class="post-modal-icon" alt="" @error="iconFailed = true" />
                            <span class="text-truncate">{{ post.feed?.title }}</span>
                            <span v-if="post.author" class="text-truncate">&middot; {{ post.author }}</span>
                            <span v-if="post.pubDate" class="flex-shrink-0">&middot; {{ new Date(post.pubDate).toLocaleString() }}</span>
                        </div>
                        <div class="post-modal-actions">
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
                        <iframe :src="youtubeEmbedUrl" :title="post.title" allowfullscreen
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
                    </a>
                    <div v-if="contentLoading" class="post-modal-loading">
                        <span class="spinner-border spinner-border-sm" role="status"></span>
                        {{ language.get('Loading full article...') || 'Loading full article...' }}
                    </div>
                    <div v-else-if="fullContent || post.description" class="post-modal-content" v-html="fullContent || post.description"
                        @error.capture="hideBrokenImage"></div>
                </article>
            </div>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { postContentStore } from '@/functions/postContentStore';
import { hideBrokenImage } from '@/functions/hideBrokenImage';
import { youtubeEmbedStore } from '@/functions/youtubeEmbedStore';
import LiveBadge from '@/components/LiveBadge.vue';
import { language } from '@/functions/languageStore';
import { computed, ref, watch } from 'vue';

const props = defineProps<{
    post: PostType;
}>();

const emit = defineEmits<{
    'close': []
    'toggle-saved': [postId: string]
    'show-logs': [post: PostType]
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
const contentLoading = computed(() => postContentStore.isLoading(props.post.id));
// Log entries of the post, e.g. when its page couldn't be read
const logCount = computed(() => postContentStore.logCount(props.post.id));

// Load the full article (videos show the player instead) and start at the top when switching posts
watch(() => props.post.id, () => {
    iconFailed.value = false;
    bodyRef.value?.scrollTo({ top: 0 });
    if (videoId.value) youtubeEmbedStore.check(videoId.value);
    else postContentStore.load(props.post.id);
}, { immediate: true });

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

.post-modal-close:hover {
    background-color: rgba(127, 127, 127, 0.3);
}

.post-modal-article {
    max-width: 860px;
    margin: 0 auto;
    padding: 4rem 1.5rem 5rem;
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
    .post-modal-article {
        padding-top: 4.5rem;
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
</style>
