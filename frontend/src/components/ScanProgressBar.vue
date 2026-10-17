<template>
    <!-- Thin progress line at the bottom of the page, shown while the feeds are scanned -->
    <div v-if="visible" class="scan-progress" role="progressbar" :aria-valuenow="percent ?? undefined"
        aria-valuemin="0" aria-valuemax="100" :title="label">
        <div class="scan-progress-bar" :class="{ indeterminate: percent === null }"
            :style="percent !== null ? { width: percent + '%' } : undefined"></div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { getFeedOperationStatus } from '@/api/feed';
import { language } from '@/functions/languageStore';
import { scanStore } from '@/functions/scanStore';

const RUNNING_INTERVAL = 1000;
const IDLE_INTERVAL = 5000; // The scheduler scans in the background, so the status is checked while idle too

const isRunning = ref(false);
const total = ref(0);
const completed = ref(0);
let timer: number | null = null;

// Hidden as soon as all the feeds are checked, without waiting for the scan to wrap up
const visible = computed(() => isRunning.value && (total.value === 0 || completed.value < total.value));

// null when the progress isn't known (e.g. during an OPML import)
const percent = computed<number | null>(() => total.value > 0
    ? Math.min(100, Math.round(completed.value * 100 / total.value))
    : null);

const label = computed(() => total.value > 0
    ? `${language.get('Scanning feeds') || 'Scanning feeds'}: ${completed.value} / ${total.value}`
    : language.get('Scanning feeds') || 'Scanning feeds');

const checkStatus = async () => {
    try {
        const status = await getFeedOperationStatus();
        const wasRunning = isRunning.value;
        isRunning.value = !!status?.isRunning;
        total.value = status?.total ?? 0;
        completed.value = status?.completed ?? 0;
        if (wasRunning && !isRunning.value) {
            scanStore.finishedCount.value++;
        }
    } catch (e) {
        console.warn('Could not check the scan status:', e);
    }
    schedule();
};

const schedule = () => {
    if (timer) clearTimeout(timer);
    timer = window.setTimeout(checkStatus, isRunning.value || scanStore.active.value ? RUNNING_INTERVAL : IDLE_INTERVAL);
};

// A scan started from the page shows up right away instead of on the next idle check,
// and its end is picked up as soon as it returns
watch(scanStore.active, (active) => {
    if (active) schedule();
    else checkStatus();
});

onMounted(checkStatus);

onUnmounted(() => {
    if (timer) clearTimeout(timer);
});
</script>

<style scoped>
.scan-progress {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    height: 3px;
    z-index: 1060;
    background-color: rgba(var(--bs-primary-rgb), 0.15);
    overflow: hidden;
}

.scan-progress-bar {
    height: 100%;
    width: 0;
    background-color: var(--bs-primary);
    transition: width 0.4s ease;
}

/* Sliding segment when the progress isn't known */
.scan-progress-bar.indeterminate {
    width: 30%;
    transition: none;
    animation: scan-progress-slide 1.2s ease-in-out infinite;
}

@keyframes scan-progress-slide {
    from { transform: translateX(-100%); }
    to { transform: translateX(340%); }
}
</style>
