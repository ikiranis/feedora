<template>
    <!-- Shows the article of the link preview's page inside the post; the parent renders it in the slot, with its own content styles -->
    <div class="link-content">
        <button type="button" class="link-content-toggle" :aria-expanded="shown" @click.stop="linkContentStore.toggle(postId)"
            :title="(language.get('Show the article of the link inside the post') || 'Show the article of the link inside the post') + ' (W)'">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
                <path v-if="shown" fill-rule="evenodd" d="M7.646 4.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1-.708.708L8 5.707l-5.646 5.647a.5.5 0 0 1-.708-.708z"/>
                <path v-else fill-rule="evenodd" d="M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708"/>
            </svg>
            {{ shown ? (language.get('Hide the article') || 'Hide the article') : (language.get('Show the article') || 'Show the article') }}
        </button>
        <template v-if="shown">
            <div v-if="loading" class="link-content-status">
                <span class="spinner-border spinner-border-sm" role="status"></span>
                {{ language.get('Loading full article...') || 'Loading full article...' }}
            </div>
            <slot v-else-if="content" :html="content" />
            <div v-else class="link-content-status">
                {{ language.get('The article of the link couldn\'t be loaded') || 'The article of the link couldn\'t be loaded' }}
            </div>
        </template>
    </div>
</template>

<script setup lang="ts">
import { linkContentStore } from '@/functions/linkContentStore';
import { language } from '@/functions/languageStore';
import { computed } from 'vue';

const props = defineProps<{
    postId: string;
}>();

const shown = computed(() => linkContentStore.isShown(props.postId));
const loading = computed(() => linkContentStore.isLoading(props.postId));
const content = computed(() => linkContentStore.get(props.postId) ?? null);
</script>

<style scoped>
.link-content-toggle {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    margin: 0.75rem 0 1rem;
    padding: 0.35rem 0.85rem;
    border: 1px solid var(--border-color);
    border-radius: 999px;
    background: transparent;
    color: #8b919c;
    font-size: 0.9rem;
    transition: border-color 0.2s, color 0.2s;
}

.link-content-toggle:hover {
    border-color: #4d8dff;
    color: var(--text-color);
}

.link-content-status {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: #8b919c;
    font-size: 1.05rem;
}
</style>
