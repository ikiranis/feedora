<template>
    <!-- Teleported and stacked above the other modals (e.g. the post modal), with its own dimmed background -->
    <Teleport to="body">
        <div class="modal fade logs-modal" ref="modalRef" tabindex="-1" aria-labelledby="logsModalLabel" aria-hidden="true"
             @click.self="getModal().hide()">
            <div class="modal-dialog modal-xl modal-dialog-scrollable">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title text-truncate" id="logsModalLabel">
                            {{ language.get('Logs') }}: {{ title }}
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
                            <span v-if="postFilter" class="badge text-bg-secondary filter-badge">
                                {{ language.get('Post') }}: {{ postFilterTitle || postFilter }}
                                <button type="button" class="btn-close btn-close-white ms-1" @click="postFilter = null" :aria-label="language.get('Remove filter')"></button>
                            </span>
                        </div>

                        <!-- All the entries are of the same feed (or post), so its column is hidden -->
                        <LogTable v-if="logs.length > 0" :logs="logs" :show-feed="false" :show-post="!postId"
                                  @filter="(_key, id) => postFilter = id" />
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
                                :title="postId ? language.get('Delete the log entries of this post') : language.get('Delete the log entries of this feed')">
                            {{ language.get('Clear logs') }}
                        </button>
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
import { getLogs, deleteLogs } from '@/api/log';
import type { LogEntryType, LogLevel } from '@/types';
import { language } from '@/functions/languageStore';
import { errorStore } from '@/components/error/errorStore';
import LogTable from '@/components/log/LogTable.vue';

// Shows the log entries of a post if postId is given, otherwise those of the feed (including its posts)
interface Props {
    feedId?: string | null;
    postId?: string | null;
    title?: string;
    show?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    feedId: null,
    postId: null,
    title: '',
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
// Post selected in the table of a feed's entries
const postFilter = ref<string | null>(null);

const postFilterTitle = computed(() => logs.value.find(log => log.postId === postFilter.value)?.postTitle ?? null);

// No backdrop element: the modal's own background dims the page, so it also covers the post modal
const getModal = () => (window as any).bootstrap.Modal.getOrCreateInstance(modalRef.value, { backdrop: false });

/**
 * Fetches the log entries, the next page or, with reset, the first one.
 * @param {boolean} [reset=false] - If true, clears the loaded entries and starts from the first page.
 */
const fetchLogs = async (reset = false) => {
    if (!props.feedId && !props.postId) return;

    try {
        loading.value = true;
        if (reset) {
            logs.value = [];
            page.value = 1;
            allLoaded.value = false;
        }

        const data = await getLogs(page.value, pageSize, props.postId ? null : props.feedId, props.postId || postFilter.value, level.value);
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

watch([level, postFilter], () => fetchLogs(true));

watch(() => props.show, (show) => {
    if (show) {
        // Resetting changed filters already refetches, through the watcher above
        const filtered = level.value !== null || postFilter.value !== null;
        level.value = null;
        postFilter.value = null;
        if (!filtered) {
            fetchLogs(true);
        }
        getModal().show();
    } else {
        getModal().hide();
    }
});

/**
 * Deletes all the log entries of the post or feed, after confirmation.
 */
const clearLogs = async () => {
    const question = props.postId
        ? language.get('Are you sure you want to delete the log entries of this post')
        : language.get('Are you sure you want to delete the log entries of this feed');
    if (!confirm(question)) {
        return;
    }

    try {
        await deleteLogs(props.feedId, props.postId);
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
.logs-modal {
    z-index: 1070;
    background-color: rgba(0, 0, 0, 0.5);
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
</style>
