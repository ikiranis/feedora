<template>
    <div class="container py-3">
        <div class="row justify-content-center">
            <div class="col-auto">
                <div class="d-flex align-items-center gap-3 flex-wrap">
                    <select v-model="level" class="form-select" style="width: 180px;" :title="language.get('Filter by level')">
                        <option :value="null">{{ language.get('All levels') }}</option>
                        <option value="ERROR">{{ language.get('Errors') }}</option>
                        <option value="WARNING">{{ language.get('Warnings') }}</option>
                        <option value="INFO">{{ language.get('Info') }}</option>
                    </select>
                    <span v-if="feedId" class="badge text-bg-secondary filter-badge">
                        {{ language.get('Feed') }}: {{ feedTitle || feedId }}
                        <button type="button" class="btn-close btn-close-white ms-1" @click="clearFilter('feedId')" :aria-label="language.get('Remove filter')"></button>
                    </span>
                    <span v-if="postId" class="badge text-bg-secondary filter-badge">
                        {{ language.get('Post') }}: {{ postTitle || postId }}
                        <button type="button" class="btn-close btn-close-white ms-1" @click="clearFilter('postId')" :aria-label="language.get('Remove filter')"></button>
                    </span>
                    <button class="btn btn-danger" @click="clearLogs" :disabled="logs.length === 0"
                            :title="feedId ? language.get('Delete the log entries of this feed') : language.get('Delete all log entries')">
                        {{ language.get('Clear logs') }}
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="scrollable-log-view" ref="scrollableContainerRef">
        <div class="container py-4">
            <div v-if="logs.length > 0" class="table-responsive">
                <table class="table table-bordered table-hover align-middle">
                    <thead class="table-light custom-table-header">
                        <tr>
                            <th>{{ language.get('Date') }}</th>
                            <th>{{ language.get('Level') }}</th>
                            <th>{{ language.get('Feed') }}</th>
                            <th>{{ language.get('Post') }}</th>
                            <th>{{ language.get('Message') }}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="log in logs" :key="log.id">
                            <td class="text-nowrap">{{ formatDate(log.createdAt) }}</td>
                            <td>
                                <span :class="'badge ' + levelClass(log.level)">{{ log.level }}</span>
                                <div class="small text-muted">{{ log.type }}</div>
                            </td>
                            <td>
                                <a v-if="log.feedId" href="#" class="text-decoration-none" @click.prevent="filterBy('feedId', log.feedId)"
                                   :title="language.get('Show the log entries of this feed')">
                                    {{ log.feedTitle || log.feedId }}
                                </a>
                            </td>
                            <td>
                                <a v-if="log.postId" href="#" class="text-decoration-none" @click.prevent="filterBy('postId', log.postId)"
                                   :title="language.get('Show the log entries of this post')">
                                    {{ log.postTitle || log.postId }}
                                </a>
                            </td>
                            <td class="log-message">
                                <div>{{ log.message }}</div>
                                <a v-if="log.url" :href="log.url" target="_blank" class="small text-decoration-none text-break">{{ log.url }}</a>
                                <details v-if="log.details" class="small mt-1">
                                    <summary>{{ language.get('Details') }}</summary>
                                    <pre class="mb-0 log-details">{{ log.details }}</pre>
                                </details>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div v-if="allLoaded" class="text-center w-100 text-muted my-4">
                    {{ language.get('No more log entries') }}
                </div>
            </div>
            <div v-else-if="!loading" class="text-center text-muted mt-5">
                {{ language.get('No log entries found') }}
            </div>

            <div v-if="loading" class="loading-spinner-fixed">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        </div>
    </div>

    <Error class="error-fixed-bottom" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getLogs, deleteLogs } from '@/api/log';
import type { LogEntryType, LogLevel } from '@/types';
import { language } from '@/functions/languageStore';
import Error from '@/components/error/Error.vue';
import { errorStore } from '@/components/error/errorStore';

const route = useRoute();
const router = useRouter();

const logs = ref<LogEntryType[]>([]);
const scrollableContainerRef = ref<HTMLElement | null>(null);
const loading = ref(false);
const page = ref(1);
const pageSize = 50;
const allLoaded = ref(false);
const level = ref<LogLevel | null>(null);

// The feed and post filters live in the route query, so other views can link to /logs?feedId=...
const feedId = computed(() => (route.query.feedId as string) || null);
const postId = computed(() => (route.query.postId as string) || null);

// Titles for the filter badges, taken from the loaded entries
const feedTitle = computed(() => logs.value.find(log => log.feedId === feedId.value)?.feedTitle ?? null);
const postTitle = computed(() => logs.value.find(log => log.postId === postId.value)?.postTitle ?? null);

/**
 * Fetches log entries from the API, the next page or, with reset, the first one.
 * @param {boolean} [reset=false] - If true, clears the loaded entries and starts from the first page.
 */
const fetchLogs = async (reset = false) => {
    try {
        loading.value = true;
        if (reset) {
            page.value = 1;
            allLoaded.value = false;
        }

        const data = await getLogs(page.value, pageSize, feedId.value, postId.value, level.value);
        if (data.length < pageSize) {
            allLoaded.value = true;
        }

        logs.value = reset ? data : logs.value.concat(data);
        page.value++;
    } catch (e: any) {
        if (e.response && e.response.data && e.response.data.message && typeof e.response.data.status === 'number') {
            errorStore.set(true, e.response.data.message, e.response.data.status);
        } else {
            errorStore.set(true, language.get('Failed to fetch log entries') || 'Failed to fetch log entries.', 500);
        }
    } finally {
        loading.value = false;
    }
};

watch([level, feedId, postId], () => fetchLogs(true));

/**
 * Shows only the entries of a feed or post. Filtering by post drops the feed filter and vice versa.
 */
const filterBy = (key: 'feedId' | 'postId', id: string) => {
    router.push({ name: 'Logs', query: { [key]: id } });
};

const clearFilter = (key: 'feedId' | 'postId') => {
    const query = { ...route.query };
    delete query[key];
    router.push({ name: 'Logs', query });
};

/**
 * Deletes the log entries of the filtered feed or, without a feed filter, all of them, after confirmation.
 */
const clearLogs = async () => {
    const question = feedId.value
        ? language.get('Are you sure you want to delete the log entries of this feed')
        : language.get('Are you sure you want to delete all log entries');
    if (!confirm(question)) {
        return;
    }

    try {
        await deleteLogs(feedId.value);
        await fetchLogs(true);
    } catch (e: any) {
        errorStore.set(true, language.get('Failed to delete log entries') || 'Failed to delete log entries.', 500);
    }
};

const formatDate = (date: string) => new Date(date).toLocaleString();

const levelClass = (logLevel: LogLevel) => {
    switch (logLevel) {
        case 'ERROR': return 'text-bg-danger';
        case 'WARNING': return 'text-bg-warning';
        default: return 'text-bg-info';
    }
};

/**
 * Loads more entries when the user scrolls near the bottom.
 */
const handleScroll = () => {
    if (loading.value || allLoaded.value || !scrollableContainerRef.value) return;

    const container = scrollableContainerRef.value;
    if (container.scrollTop + container.clientHeight + 200 >= container.scrollHeight) {
        fetchLogs();
    }
};

onMounted(() => {
    fetchLogs(true);
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.addEventListener('scroll', handleScroll);
    }
    document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.removeEventListener('scroll', handleScroll);
    }
    document.body.style.overflow = 'auto';
});
</script>

<style scoped>
.scrollable-log-view {
    height: 80vh;
    overflow-y: auto;
}

.filter-badge {
    display: inline-flex;
    align-items: center;
    font-size: 0.9rem;
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.log-message {
    min-width: 300px;
}

.log-details {
    white-space: pre-wrap;
    word-break: break-word;
    max-height: 300px;
    overflow-y: auto;
}

.loading-spinner-fixed {
    position: fixed;
    bottom: 20px;
    right: 20px;
    z-index: 1000;
}

.error-fixed-bottom {
    position: fixed;
    bottom: 5em;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 1000;
}

[data-bs-theme="dark"] .custom-table-header {
    background-color: #343a40 !important;
    color: #ffffff !important;
}

[data-bs-theme="dark"] .custom-table-header th {
    background-color: #343a40 !important;
    color: #ffffff !important;
    border-color: #495057 !important;
}
</style>
