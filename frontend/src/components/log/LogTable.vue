<template>
    <div class="table-responsive">
        <table class="table table-bordered table-hover align-middle">
            <thead class="table-light custom-table-header">
                <tr>
                    <th>{{ language.get('Date') }}</th>
                    <th>{{ language.get('Level') }}</th>
                    <th v-if="showFeed">{{ language.get('Feed') }}</th>
                    <th v-if="showPost">{{ language.get('Post') }}</th>
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
                    <td v-if="showFeed">
                        <a v-if="log.feedId && clickable" href="#" class="text-decoration-none" @click.prevent="emit('filter', 'feedId', log.feedId)"
                           :title="language.get('Show the log entries of this feed')">
                            {{ log.feedTitle || log.feedId }}
                        </a>
                        <span v-else-if="log.feedId">{{ log.feedTitle || log.feedId }}</span>
                    </td>
                    <td v-if="showPost">
                        <a v-if="log.postId && clickable" href="#" class="text-decoration-none" @click.prevent="emit('filter', 'postId', log.postId)"
                           :title="language.get('Show the log entries of this post')">
                            {{ log.postTitle || log.postId }}
                        </a>
                        <span v-else-if="log.postId">{{ log.postTitle || log.postId }}</span>
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
    </div>
</template>

<script setup lang="ts">
import type { LogEntryType, LogLevel } from '@/types';
import { language } from '@/functions/languageStore';
import { dateFormatStore } from '@/functions/dateFormatStore';

interface Props {
    logs: LogEntryType[];
    // Show the feed column. Hidden when all the entries are of the same feed.
    showFeed?: boolean;
    // Show the post column. Hidden when all the entries are of the same post.
    showPost?: boolean;
    // Make the feed and post titles links that emit 'filter'
    clickable?: boolean;
}

withDefaults(defineProps<Props>(), {
    showFeed: true,
    showPost: true,
    clickable: true
});

const emit = defineEmits<{
    filter: [key: 'feedId' | 'postId', id: string];
}>();

const formatDate = (date: string) => dateFormatStore.dateTime(date, true);

const levelClass = (logLevel: LogLevel) => {
    switch (logLevel) {
        case 'ERROR': return 'text-bg-danger';
        case 'WARNING': return 'text-bg-warning';
        default: return 'text-bg-info';
    }
};
</script>

<style scoped>
.log-message {
    min-width: 300px;
}

.log-details {
    white-space: pre-wrap;
    word-break: break-word;
    max-height: 300px;
    overflow-y: auto;
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
