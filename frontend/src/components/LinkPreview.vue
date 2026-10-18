<template>
    <!-- Preview of the link in a tweet: compact (image beside the text) on cards, large (image above) when opened -->
    <a :href="preview.url" target="_blank" rel="noopener noreferrer" class="link-preview" :class="{ 'link-preview-large': large }"
        :title="preview.url" @click.stop>
        <img v-if="preview.image && !hideImage && !imageFailed" :src="preview.image" class="link-preview-image" alt="" loading="lazy"
            @error="imageFailed = true" />
        <span class="link-preview-text">
            <span v-if="preview.title" class="link-preview-title">{{ preview.title }}</span>
            <span v-if="preview.host" class="link-preview-host">{{ preview.host }}</span>
        </span>
    </a>
</template>

<script setup lang="ts">
import type { LinkPreview } from '@/functions/tweet';
import { ref, watch } from 'vue';

const props = defineProps<{
    preview: LinkPreview;
    large?: boolean;
    // e.g. when the card already shows the same image
    hideImage?: boolean;
}>();

const imageFailed = ref(false);
watch(() => props.preview.image, () => imageFailed.value = false);
</script>

<style scoped>
.link-preview {
    display: flex;
    align-items: stretch;
    margin-top: 0.85rem;
    border: 1px solid var(--border-color);
    border-radius: 0.75rem;
    overflow: hidden;
    color: inherit;
    text-decoration: none;
    transition: border-color 0.2s, background-color 0.2s;
}

[data-bs-theme="dark"] .link-preview {
    border-color: #2a2e36;
}

.link-preview:hover {
    border-color: #4d8dff;
    background-color: rgba(77, 141, 255, 0.06);
}

.link-preview-image {
    width: 5.5rem;
    min-height: 5.5rem;
    flex-shrink: 0;
    object-fit: cover;
    background-color: #000;
}

.link-preview-text {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 0.25rem;
    min-width: 0;
    padding: 0.6rem 0.8rem;
}

.link-preview-title {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 0.92rem;
    font-weight: 600;
    line-height: 1.35;
}

.link-preview-host {
    overflow: hidden;
    color: #8b919c;
    font-size: 0.82rem;
    white-space: nowrap;
    text-overflow: ellipsis;
}

/* Large: the image across the top, like Twitter's large link card */
.link-preview-large {
    flex-direction: column;
    max-width: 40rem;
    margin-top: 1.5rem;
}

.link-preview-large .link-preview-image {
    width: 100%;
    min-height: 0;
    aspect-ratio: 1.91 / 1;
}

.link-preview-large .link-preview-text {
    padding: 0.85rem 1rem;
}

.link-preview-large .link-preview-title {
    font-size: 1.1rem;
}

.link-preview-large .link-preview-host {
    font-size: 0.9rem;
}
</style>
