<template>
    <Teleport to="body">
        <div class="post-modal-backdrop" @click.self="emit('close')">
            <div class="post-modal" role="dialog" aria-modal="true" :aria-label="post.title">
                <div class="post-modal-header">
                    <div style="min-width: 0">
                        <a :href="post.link" target="_blank" class="post-modal-title">{{ post.title }}</a>
                        <div class="post-modal-meta">
                            <img v-if="siteIcon" :src="siteIcon" class="post-modal-icon" alt="" @error="iconFailed = true" />
                            <span class="text-truncate">{{ post.feed?.title }}</span>
                            <span v-if="post.author" class="text-truncate">&middot; {{ post.author }}</span>
                            <span v-if="post.pubDate" class="flex-shrink-0">&middot; {{ new Date(post.pubDate).toLocaleString() }}</span>
                        </div>
                    </div>
                    <div class="d-flex align-items-start gap-2 flex-shrink-0">
                        <button type="button" class="post-modal-action" :class="{ saved: post.saved }" @click="emit('toggle-saved', post.id)"
                            :title="post.saved ? (language.get('Remove from saved') || 'Remove from saved') : (language.get('Save for later') || 'Save for later')">
                            <svg v-if="post.saved" xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                                <path d="M2 2v13.5a.5.5 0 0 0 .74.439L8 13.069l5.26 2.87A.5.5 0 0 0 14 15.5V2a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z"/>
                            </svg>
                            <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                                <path d="M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v13.5a.5.5 0 0 1-.777.416L8 13.101l-5.223 2.815A.5.5 0 0 1 2 15.5V2zm2-1a1 1 0 0 0-1 1v12.566l4.723-2.482a.5.5 0 0 1 .554 0L13 14.566V2a1 1 0 0 0-1-1H4z"/>
                            </svg>
                        </button>
                        <a :href="post.link" target="_blank" class="post-modal-action" :title="language.get('Open original') || 'Open original'">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5z"/>
                                <path fill-rule="evenodd" d="M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0v-5z"/>
                            </svg>
                        </a>
                        <button type="button" class="btn-close ms-1" :aria-label="language.get('Close') || 'Close'" @click="emit('close')"></button>
                    </div>
                </div>

                <div ref="bodyRef" class="post-modal-body">
                    <div v-if="youtubeEmbedUrl" class="ratio ratio-16x9 mb-3">
                        <iframe :src="youtubeEmbedUrl" :title="post.title" allowfullscreen
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe>
                    </div>
                    <div v-if="contentLoading" class="post-modal-loading">
                        <span class="spinner-border spinner-border-sm" role="status"></span>
                        {{ language.get('Loading full article...') || 'Loading full article...' }}
                    </div>
                    <div v-else-if="fullContent || post.description" class="post-modal-content" v-html="fullContent || post.description"></div>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { postContentStore } from '@/functions/postContentStore';
import { language } from '@/functions/languageStore';
import { computed, ref, watch } from 'vue';

const props = defineProps<{
    post: PostType;
}>();

const emit = defineEmits<{
    'close': []
    'toggle-saved': [postId: string]
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
 * YouTube embed URL if the post links to a video (watch, youtu.be, shorts, embed or live links).
 */
const youtubeEmbedUrl = computed(() => {
    try {
        const url = new URL(props.post.link);
        const host = url.hostname.replace(/^(www\.|m\.)/, '');
        let videoId: string | null = null;
        if (host === 'youtu.be') {
            videoId = url.pathname.slice(1).split('/')[0];
        } else if (host === 'youtube.com' || host === 'youtube-nocookie.com') {
            videoId = url.pathname.match(/^\/(?:shorts|embed|live)\/([\w-]+)/)?.[1] ?? url.searchParams.get('v');
        }
        return videoId && /^[\w-]{11}$/.test(videoId) ? `https://www.youtube-nocookie.com/embed/${videoId}` : null;
    } catch {
        return null;
    }
});

const fullContent = computed(() => postContentStore.get(props.post.id) ?? null);
const contentLoading = computed(() => postContentStore.isLoading(props.post.id));

// Load the full article (videos show the player instead) and start at the top when switching posts
watch(() => props.post.id, () => {
    iconFailed.value = false;
    bodyRef.value?.scrollTo({ top: 0 });
    if (!youtubeEmbedUrl.value) postContentStore.load(props.post.id);
}, { immediate: true });

/**
 * Scrolls the modal body, used by the keyboard handler of the posts view.
 */
const scrollBy = (top: number) => bodyRef.value?.scrollBy({ top });

defineExpose({ scrollBy });
</script>

<style scoped>
.post-modal-backdrop {
    position: fixed;
    inset: 0;
    z-index: 1055;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
    background-color: rgba(0, 0, 0, 0.6);
}

.post-modal {
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: 900px;
    max-height: 100%;
    border: 1px solid var(--border-color);
    border-radius: 1rem;
    background-color: var(--card-bg);
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
    overflow: hidden;
}

[data-bs-theme="dark"] .post-modal {
    background-color: #16181d;
    border-color: #2a2e36;
}

.post-modal-header {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    padding: 1.25rem 1.5rem 1rem;
    border-bottom: 1px solid var(--border-color);
}

.post-modal-title {
    font-size: 1.35rem;
    font-weight: 700;
    line-height: 1.35;
    color: var(--text-color);
    text-decoration: none;
}

.post-modal-title:hover {
    color: #4d8dff;
}

.post-modal-meta {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin-top: 0.5rem;
    color: #8b919c;
    font-size: 0.9rem;
    min-width: 0;
}

.post-modal-icon {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    border-radius: 50%;
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

.post-modal-action.saved {
    color: #4d8dff;
}

.post-modal-body {
    padding: 1.25rem 1.5rem 1.5rem;
    overflow-y: auto;
}

.ratio iframe {
    border: 0;
    border-radius: 0.5rem;
}

.post-modal-loading {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: #8b919c;
}

/* Full article content: readable line length and paragraph spacing */
.post-modal-content {
    max-width: 80ch;
    margin: 0 auto;
    font-size: 1.05rem;
    line-height: 1.7;
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
    margin: 0.5rem 0;
}

.post-modal-content :deep(p) {
    margin: 0 0 1em;
}

.post-modal-content :deep(h1),
.post-modal-content :deep(h2),
.post-modal-content :deep(h3),
.post-modal-content :deep(h4) {
    margin: 1.4em 0 0.6em;
    font-size: 1.2rem;
    font-weight: 600;
}

.post-modal-content :deep(ul),
.post-modal-content :deep(ol) {
    margin: 0 0 1em;
    padding-left: 1.5em;
}

.post-modal-content :deep(blockquote) {
    margin: 0 0 1em;
    padding-left: 1em;
    border-left: 3px solid var(--border-color);
    color: #8b919c;
}

.post-modal-content :deep(figure) {
    margin: 0 0 1em;
}

.post-modal-content :deep(figcaption) {
    font-size: 0.85rem;
    color: #8b919c;
}
</style>
