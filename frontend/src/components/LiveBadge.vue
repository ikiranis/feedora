<template>
    <span v-if="post.liveStatus" class="live-badge" :class="post.liveStatus === 'LIVE' ? 'is-live' : 'is-upcoming'" :title="tooltip">
        <span v-if="post.liveStatus === 'LIVE'" class="live-dot"></span>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="currentColor" viewBox="0 0 16 16">
            <path d="M8 3.5a.5.5 0 0 0-1 0V9a.5.5 0 0 0 .252.434l3.5 2a.5.5 0 0 0 .496-.868L8 8.71z"/>
            <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16m7-8A7 7 0 1 1 1 8a7 7 0 0 1 14 0"/>
        </svg>
        {{ label }}
    </span>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { language } from '@/functions/languageStore';
import { dateFormatStore } from '@/functions/dateFormatStore';
import { computed } from 'vue';

const props = defineProps<{
    post: PostType;
}>();

const startAt = computed(() => props.post.liveStartAt ? new Date(props.post.liveStartAt) : null);

/**
 * "Live", or "Upcoming" with the start time (only the time for today, otherwise the day too).
 */
const label = computed(() => {
    if (props.post.liveStatus === 'LIVE') return language.get('Live') || 'Live';

    const upcoming = language.get('Upcoming') || 'Upcoming';
    if (!startAt.value || startAt.value < new Date()) return upcoming;
    const time = dateFormatStore.time(startAt.value);
    return startAt.value.toDateString() === new Date().toDateString()
        ? `${upcoming} · ${time}`
        : `${upcoming} · ${dateFormatStore.shortDate(startAt.value)}, ${time}`;
});

const tooltip = computed(() => {
    if (!startAt.value) return '';
    const date = dateFormatStore.dateTime(startAt.value);
    return props.post.liveStatus === 'LIVE'
        ? `${language.get('Live since') || 'Live since'} ${date}`
        : `${language.get('Scheduled for') || 'Scheduled for'} ${date}`;
});
</script>

<style scoped>
.live-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    flex-shrink: 0;
    padding: 0.1rem 0.45rem;
    border-radius: 0.35rem;
    font-size: 0.75rem;
    font-weight: 700;
    line-height: 1.4;
    white-space: nowrap;
    vertical-align: middle;
}

.live-badge.is-live {
    background-color: #e62117;
    color: #fff;
    text-transform: uppercase;
}

.live-badge.is-upcoming {
    background-color: rgba(77, 141, 255, 0.15);
    color: #4d8dff;
}

.live-dot {
    width: 0.45rem;
    height: 0.45rem;
    border-radius: 50%;
    background-color: currentColor;
    animation: live-pulse 1.5s ease-in-out infinite;
}

@keyframes live-pulse {
    50% { opacity: 0.3; }
}
</style>
