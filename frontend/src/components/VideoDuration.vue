<template>
    <span v-if="label" class="video-duration">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
    // Length in seconds, null if unknown
    seconds: number | null | undefined;
}>();

/**
 * Length as YouTube shows it: "m:ss", or "h:mm:ss" from an hour on.
 */
const label = computed(() => {
    if (!props.seconds) return '';
    const hours = Math.floor(props.seconds / 3600);
    const minutes = Math.floor((props.seconds % 3600) / 60);
    const seconds = String(props.seconds % 60).padStart(2, '0');
    return hours > 0 ? `${hours}:${String(minutes).padStart(2, '0')}:${seconds}` : `${minutes}:${seconds}`;
});
</script>

<style scoped>
/* Bottom right corner of the thumbnail, which must be positioned (the sizes undo Bootstrap's .ratio > *) */
.video-duration {
    position: absolute;
    top: auto;
    left: auto;
    right: 0.5rem;
    bottom: 0.5rem;
    width: auto;
    height: auto;
    padding: 0.05rem 0.35rem;
    border-radius: 0.3rem;
    background-color: rgba(0, 0, 0, 0.8);
    color: #fff;
    font-size: 0.75rem;
    font-weight: 600;
    line-height: 1.4;
    font-variant-numeric: tabular-nums;
    pointer-events: none;
}
</style>
