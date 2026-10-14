<template>
    <div class="modal fade" id="feedLogsModal" ref="modalRef" tabindex="-1" aria-labelledby="feedLogsModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-xl modal-dialog-scrollable">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title text-truncate" id="feedLogsModalLabel">
                        {{ language.get('Logs') }}: {{ feed?.title }}
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="d-flex align-items-center gap-2 mb-3">
                        <select v-model="level" class="form-select" style="width: 180px;" :title="language.get('Filter by level')">
                            <option :value="null">{{ language.get('All levels') }}</option>
                            <option value="ERROR">{{ language.get('Errors') }}</option>
                            <option value="WARNING">{{ language.get('Warnings') }}</option>
                            <option value="INFO">{{ language.get('Info') }}</option>
                        </select>
                        <span v-if="postId" class="badge text-bg-secondary filter-badge">
                            {{ language.get('Post') }}: {{ postTitle || postId }}
                            <button type="button" class="btn-close btn-close-white ms-1" @click="postId = null" :aria-label="language.get('Remove filter')"></button>
                        </span>
                    </div>

                    <LogTable v-if="logs.length > 0" :logs="logs" :show-feed="false" @filter="(_key, id) => postId = id" />
                    <div v-else-if="!loading" class="text-center text-muted my-4">
                        {{ language.get('No log entries found') }}
                    </div>

                    <div class="text-center">
                        <div v-if="loading" class="spinner-border text-primary" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                        <button v-else-if="!allLoaded && logs.length > 0" class="btn btn-sm btn-secondary" @click="fetchLogs()">
                            {{ language.get('Load more') }}
                        </button>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-danger me-auto" @click="clearLogs" :disabled="logs.length === 0"
                            :title="language.get('Delete the log entries of this feed')">
                        {{ language.get('Clear logs') }}
                    </button>
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                        {{ language.get('Close') }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { getLogs, deleteLogs } from '@/api/log';
import type { Feed, LogEntryType, LogLevel } from '@/types';
import { language } from '@/functions/languageStore';
import { errorStore } from '@/components/error/errorStore';
import LogTable from '@/components/log/LogTable.vue';

interface Props {
    feed?: Feed | null;
    show?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    feed: null,
    show: false
});

const emit = defineEmits<{
    'logs-cleared': [];
    'modal-hidden': [];
}>();

const modalRef = ref<HTMLElement | null>(null);
const logs = ref<LogEntryType[]>([]);
const loading = ref(false);
const page = ref(1);
const pageSize = 50;
const allLoaded = ref(false);
const level = ref<LogLevel | null>(null);
const postId = ref<string | null>(null);

const postTitle = computed(() => logs.value.find(log => log.postId === postId.value)?.postTitle ?? null);

const getModal = () => (window as any).bootstrap.Modal.getOrCreateInstance(modalRef.value);

/**
 * Fetches the feed's log entries, the next page or, with reset, the first one.
 * @param {boolean} [reset=false] - If true, clears the loaded entries and starts from the first page.
 */
const fetchLogs = async (reset = false) => {
    if (!props.feed) return;

    try {
        loading.value = true;
        if (reset) {
            logs.value = [];
            page.value = 1;
            allLoaded.value = false;
        }

        const data = await getLogs(page.value, pageSize, props.feed.id, postId.value, level.value);
        if (data.length < pageSize) {
            allLoaded.value = true;
        }

        logs.value = logs.value.concat(data);
        page.value++;
    } catch (e: any) {
        errorStore.set(true, language.get('Failed to fetch log entries') || 'Failed to fetch log entries.', 500);
    } finally {
        loading.value = false;
    }
};

watch([level, postId], () => fetchLogs(true));

watch(() => props.show, (show) => {
    if (show) {
        // Resetting changed filters already refetches, through the watcher above
        const filtered = level.value !== null || postId.value !== null;
        level.value = null;
        postId.value = null;
        if (!filtered) {
            fetchLogs(true);
        }
        getModal().show();
    } else {
        getModal().hide();
    }
});

/**
 * Deletes all the log entries of the feed, after confirmation.
 */
const clearLogs = async () => {
    if (!props.feed || !confirm(language.get('Are you sure you want to delete the log entries of this feed'))) {
        return;
    }

    try {
        await deleteLogs(props.feed.id);
        getModal().hide();
        emit('logs-cleared');
    } catch (e: any) {
        errorStore.set(true, language.get('Failed to delete log entries') || 'Failed to delete log entries.', 500);
    }
};

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
.filter-badge {
    display: inline-flex;
    align-items: center;
    font-size: 0.9rem;
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
</style>
