<template>
    <!-- No box of its own: the buttons line up with the other actions of the parent -->
    <div class="share-actions">
        <CopyLinkButton :link="post.link" :size="size" class="share-action" />
        <a v-for="network in networks" :key="network.name" :href="network.url" target="_blank" rel="noopener noreferrer"
            class="share-action" :title="(language.get('Share on') || 'Share on') + ' ' + network.name" @click.stop>
            <svg xmlns="http://www.w3.org/2000/svg" :width="size" :height="size" fill="currentColor" viewBox="0 0 16 16">
                <path :d="network.icon"/>
            </svg>
        </a>
    </div>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import CopyLinkButton from '@/components/CopyLinkButton.vue';
import { language } from '@/functions/languageStore';
import { computed } from 'vue';

const props = withDefaults(defineProps<{
    post: PostType;
    size?: number;
}>(), {
    size: 22
});

const networks = computed(() => {
    const url = encodeURIComponent(props.post.link);
    const text = encodeURIComponent(props.post.title ?? '');
    return [
        {
            name: 'X',
            url: `https://x.com/intent/post?url=${url}&text=${text}`,
            icon: 'M12.6.75h2.454l-5.36 6.142L16 15.25h-4.937l-3.867-5.07-4.425 5.07H.316l5.733-6.57L0 .75h5.063l3.495 4.633L12.601.75Zm-.86 13.028h1.36L4.323 2.145H2.865z',
        },
        {
            name: 'Facebook',
            url: `https://www.facebook.com/sharer/sharer.php?u=${url}`,
            icon: 'M16 8.049c0-4.446-3.582-8.05-8-8.05C3.58 0-.002 3.603-.002 8.05c0 4.017 2.926 7.347 6.75 7.951v-5.625h-2.03V8.05H6.75V6.275c0-2.017 1.195-3.131 3.022-3.131.876 0 1.791.157 1.791.157v1.98h-1.009c-.993 0-1.303.621-1.303 1.258v1.51h2.218l-.354 2.326H9.25V16c3.824-.604 6.75-3.934 6.75-7.951',
        },
        {
            name: 'LinkedIn',
            url: `https://www.linkedin.com/sharing/share-offsite/?url=${url}`,
            icon: 'M0 1.146C0 .513.526 0 1.175 0h13.65C15.474 0 16 .513 16 1.146v13.708c0 .633-.526 1.146-1.175 1.146H1.175C.526 16 0 15.487 0 14.854zm4.943 12.248V6.169H2.542v7.225zm-1.2-8.212c.837 0 1.358-.554 1.358-1.248-.015-.709-.52-1.248-1.342-1.248S2.4 3.226 2.4 3.934c0 .694.521 1.248 1.327 1.248zm4.908 8.212V9.359c0-.216.016-.432.08-.586.173-.431.568-.878 1.232-.878.869 0 1.216.662 1.216 1.634v3.865h2.401V9.25c0-2.22-1.184-3.252-2.764-3.252-1.274 0-1.845.7-2.165 1.193v.025h-.016l.016-.025V6.169h-2.4c.03.678 0 7.225 0 7.225z',
        },
    ];
});
</script>

<style scoped>
.share-actions {
    display: contents;
}

.share-action {
    display: flex;
    padding: 0.25rem;
    border: 0;
    background: transparent;
    color: #8b919c;
    transition: color 0.2s;
}

.share-action:hover {
    color: var(--text-color);
}
</style>
