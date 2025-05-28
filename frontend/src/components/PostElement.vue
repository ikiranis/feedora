<template>
    <div v-if="showDetails" class="card h-100 shadow-sm custom-card" :class="{ 'unread-post': !post.read, 'read-post': post.read }" @click="handleCardClick">
        <div class="card-body d-flex flex-column">
            <h5 class="card-title">
                <a :href="post.link" target="_blank" class="post-title" @click.stop>{{ post.title }}</a>
            </h5>
            <h6 class="badge bg-dark mb-2 text-light p-1">{{ post.feed?.title }}</h6>
            <div class="mb-2" v-if="post.description">
                <div class="description-content-limiter" v-html="post.description"></div>
            </div>
            <div class="mt-auto">
                <span class="badge bg-light text-dark" v-if="post.pubDate">{{ new Date(post.pubDate).toLocaleString() }}</span>
            </div>
        </div>
    </div>
    <div v-else class="post-summary custom-summary" :class="{ 'unread-post': !post.read, 'read-post': post.read }" @click="handleCardClick">
        <div class="d-flex w-100 justify-content-between align-items-center">
            <a :href="post.link" target="_blank" class="post-title-link" @click.stop>{{ post.title }}</a>
            <span class="post-date ms-2 text-muted" v-if="post.pubDate">
                {{ new Date(post.pubDate).toLocaleString() }}
            </span>
        </div>
        <div class="summary-meta text-muted mt-1" style="font-size:0.95em;">
            <span v-if="post.feed?.title">{{ post.feed.title }}</span>
            <span v-if="post.feed?.folderName"> &mdash; {{ post.feed.folderName }}</span>
        </div>
    </div>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { markAsRead } from '@/api/post';

const props = defineProps<{
    post: PostType;
    showDetails: boolean;
}>();

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
    background-color: #343a40;
}

.custom-card.read-post {
    border: 1px solid var(--border-color);
}

.custom-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

[data-bs-theme="dark"] .custom-card:hover {
    box-shadow: 0 4px 15px rgba(255, 255, 255, 0.1);
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
    background-color: #343a40;
}

.custom-summary.read-post {
    border: 1px solid var(--border-color);
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

.post-title-link {
    text-decoration: none;
    color: #007bff;
    font-weight: 500;
    transition: color 0.3s;
}

.post-title-link:hover {
    text-decoration: underline;
}

.post-date {
    font-size: 0.85em;
    text-align: right;
    white-space: nowrap;
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
