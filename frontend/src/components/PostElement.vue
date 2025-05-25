<template>
    <div v-if="showDetails" class="card h-100 shadow-sm">
        <div class="card-body d-flex flex-column">
            <h5 class="card-title">
                <a :href="post.link" target="_blank">{{ post.title }}</a>
            </h5>
            <h6 class="card-subtitle mb-2 text-muted">{{ post.feed?.title }}</h6>
            <div class="mb-2">
                <span class="badge bg-secondary me-2">{{ post.author }}</span>
                <span class="badge bg-light text-dark">{{ post.pubDate ? new Date(post.pubDate).toLocaleString() : ''
                }}</span>
            </div>
            <div class="mb-2" v-if="post.description">
                <div class="description-content-limiter" v-html="post.description"></div>
            </div>
            <div class="mt-auto">
                <span v-if="post.read" class="badge bg-success">{{ language.get('Yes') }}</span>
                <span v-else class="badge bg-secondary">{{ language.get('No') }}</span>
            </div>
        </div>
    </div>
    <div v-else class="post-summary">
        <div class="d-flex w-100 justify-content-between align-items-center">
            <a :href="post.link" target="_blank" class="post-title-link">{{ post.title }}</a>
            <span class="post-date ms-2 text-muted" v-if="post.pubDate">
                {{ new Date(post.pubDate).toLocaleString() }}
            </span>
        </div>
        <div class="summary-meta text-muted mt-1" style="font-size:0.95em;">
            <span v-if="post.feed?.title">{{ post.feed.title }}</span>
            <span v-if="post.feed?.folderName">&mdash; {{ post.feed.folderName }}</span>
        </div>
    </div>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { language } from '@/functions/languageStore';

defineProps<{
    post: PostType;
    showDetails: boolean;
}>();
</script>

<style scoped>
.post-summary {
    padding: 0.5rem 0.75rem;
    border: 1px solid #eee;
    border-radius: 0.25rem;
    background-color: #f9f9f9;
    margin-bottom: 0.5rem;
    /* Remove flex to allow second line to break naturally */
}

.post-title-link {
    text-decoration: none;
    color: #007bff;
    font-weight: 500;
}

.post-title-link:hover {
    text-decoration: underline;
}

.post-date {
    font-size: 0.85em;
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
