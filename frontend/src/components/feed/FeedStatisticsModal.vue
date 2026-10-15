<template>
    <Teleport to="body">
        <div class="modal fade" ref="modalRef" tabindex="-1" aria-labelledby="feedStatisticsModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg modal-dialog-scrollable">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title text-truncate" id="feedStatisticsModalLabel">
                            {{ language.get('Statistics') }}: {{ feed?.title }}
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div v-if="!stats" class="text-center text-muted my-4">
                            {{ language.get('The feed has not been checked yet') }}
                        </div>
                        <template v-else>
                            <section v-for="section in sections" :key="section.title" class="mb-4">
                                <h6 class="text-uppercase text-muted small fw-semibold mb-2">{{ section.title }}</h6>
                                <table class="table table-sm mb-0 stats-table">
                                    <tbody>
                                        <tr v-for="row in section.rows" :key="row.label">
                                            <th scope="row" class="fw-normal text-muted">{{ row.label }}</th>
                                            <td :class="row.className">{{ row.value }}</td>
                                        </tr>
                                    </tbody>
                                </table>
                            </section>

                            <div v-if="stats.lastError" class="alert alert-danger mb-0">
                                <div class="small fw-semibold mb-1">{{ language.get('Last error') }}</div>
                                <div class="last-error">{{ stats.lastError }}</div>
                            </div>
                        </template>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            {{ language.get('Close') }}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import type { Feed } from '@/types';
import { language } from '@/functions/languageStore';

interface Props {
    feed?: Feed | null;
    show?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    feed: null,
    show: false
});

const emit = defineEmits<{
    'modal-hidden': [];
}>();

interface Row {
    label: string;
    value: string;
    className?: string;
}

const modalRef = ref<HTMLElement | null>(null);
const stats = computed(() => props.feed?.statistics ?? null);

const getModal = () => (window as any).bootstrap.Modal.getOrCreateInstance(modalRef.value);

const formatDate = (date: string | null) => date ? new Date(date).toLocaleString() : language.get('Never');

const formatNumber = (value: number) => value.toLocaleString(undefined, { maximumFractionDigits: 2 });

const formatInterval = (minutes: number) => minutes < 60
    ? language.get('Every {0} minutes').replace('{0}', String(minutes))
    : language.get('Every {0} hours').replace('{0}', formatNumber(minutes / 60));

const sections = computed<{ title: string, rows: Row[] }[]>(() => {
    const s = stats.value;
    if (!s) return [];

    return [
        {
            title: language.get('Frequency'),
            rows: [
                { label: language.get('Posts per day'), value: formatNumber(s.postsPerDay) },
                { label: language.get('Posts per hour'), value: formatNumber(s.postsPerHour) },
                { label: language.get('Check interval'), value: formatInterval(s.checkIntervalMinutes) },
                { label: language.get('Next check'), value: formatDate(s.nextCheckAt) },
            ]
        },
        {
            title: language.get('Updates'),
            rows: [
                { label: language.get('Last check'), value: formatDate(s.lastCheckedAt) },
                { label: language.get('Last successful check'), value: formatDate(s.lastSuccessAt) },
                { label: language.get('Last new posts found'), value: formatDate(s.lastNewPostAt) },
                { label: language.get('Newest post published'), value: formatDate(s.lastPostPubDate) },
                { label: language.get('Duration of the last check'), value: formatNumber(s.lastFetchDurationMs / 1000) + ' s' },
            ]
        },
        {
            title: language.get('Posts'),
            rows: [
                { label: language.get('Posts in the feed'), value: String(s.lastEntriesCount) },
                { label: language.get('New posts on the last check'), value: String(s.lastNewPostsCount) },
                { label: language.get('Total new posts'), value: String(s.totalNewPosts) },
            ]
        },
        {
            title: language.get('Errors'),
            rows: [
                { label: language.get('Total checks'), value: String(s.totalChecks) },
                { label: language.get('Failed checks'), value: String(s.totalErrors), className: s.totalErrors > 0 ? 'text-danger' : '' },
                { label: language.get('Failed checks in a row'), value: String(s.consecutiveErrors), className: s.consecutiveErrors > 0 ? 'text-danger fw-semibold' : '' },
                { label: language.get('Last failure'), value: formatDate(s.lastErrorAt) },
            ]
        }
    ];
});

watch(() => props.show, (show) => {
    if (show) {
        getModal().show();
    } else {
        getModal().hide();
    }
});

const handleHidden = () => emit('modal-hidden');

onMounted(() => {
    modalRef.value?.addEventListener('hidden.bs.modal', handleHidden);
});

onUnmounted(() => {
    if (modalRef.value) {
        modalRef.value.removeEventListener('hidden.bs.modal', handleHidden);
        getModal().dispose();
    }
});
</script>

<style scoped>
.stats-table th {
    width: 55%;
}

.last-error {
    white-space: pre-wrap;
    word-break: break-word;
    font-family: var(--bs-font-monospace);
    font-size: 0.85rem;
}
</style>
