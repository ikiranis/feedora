<template>
    <div class="container py-3">
        <form class="parser-toolbar d-flex flex-wrap justify-content-center align-items-center gap-2 gap-md-3" @submit.prevent="analyze">
            <input
                v-model="url"
                type="text"
                class="form-control parser-url-input"
                :placeholder="language.get('Enter the URL of a page without a feed')"
                :title="language.get('Enter the URL of a page without a feed')"
                :disabled="analyzing"
            />
            <button
                type="submit"
                class="btn btn-primary"
                style="white-space: nowrap;"
                :disabled="!url.trim() || analyzing"
                :title="language.get('Read the page and choose the element of its posts')"
            >
                <span v-if="analyzing" class="spinner-border spinner-border-sm me-2" role="status"></span>
                {{ language.get('Add') }}
            </button>
        </form>
    </div>

    <div class="scrollable-parser-view">
        <div class="container py-4">
            <div v-if="pages.length > 0" class="table-responsive">
                <table class="table table-bordered table-hover align-middle parser-table">
                    <thead class="table-light custom-table-header">
                        <tr>
                            <th>{{ language.get('Title') }}</th>
                            <th>{{ language.get('Feed') }}</th>
                            <th>{{ language.get('Element of the posts') }}</th>
                            <th class="text-end">{{ language.get('Frequency') }}</th>
                            <th class="text-end">{{ language.get('Posts') }}</th>
                            <th>{{ language.get('Last parsed') }}</th>
                            <th>{{ language.get('Actions') }}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="page in pages" :key="page.id">
                            <td class="parser-title-cell">
                                <a :href="page.url" target="_blank" rel="noopener" class="text-decoration-none" :title="page.url">
                                    {{ page.title }}
                                </a>
                                <span v-if="page.inFeeds" class="badge text-bg-success ms-1">{{ language.get('In feeds') }}</span>
                            </td>
                            <td :data-label="language.get('Feed')">
                                <a :href="feedLink(page)" target="_blank" rel="noopener" class="text-decoration-none" :title="feedLink(page)">
                                    {{ page.feedName }}.xml
                                </a>
                            </td>
                            <td :data-label="language.get('Element of the posts')">
                                <code class="selector" :title="page.selector">{{ page.selector }}</code>
                            </td>
                            <td class="text-end" :data-label="language.get('Frequency')">{{ formatFrequency(page.frequency) }}</td>
                            <td class="text-end" :data-label="language.get('Posts')">{{ page.lastParsedAt ? page.lastItemCount : '—' }}</td>
                            <td :data-label="language.get('Last parsed')">
                                {{ page.lastParsedAt ? dateFormatStore.dateTime(page.lastParsedAt) : '—' }}
                                <span v-if="page.lastError" class="badge text-bg-danger ms-1" :title="page.lastError">
                                    {{ language.get('Error') }}
                                </span>
                            </td>
                            <td class="parser-actions-cell">
                                <button
                                    class="btn btn-primary btn-sm me-2"
                                    @click="parseNow(page)"
                                    :disabled="busyPageId !== null"
                                    :title="language.get('Parse this page now')"
                                >
                                    <span v-if="busyPageId === page.id" class="spinner-border spinner-border-sm" role="status"></span>
                                    <svg v-else width="16" height="16" fill="currentColor" class="bi bi-cloud-arrow-down" viewBox="0 0 16 16">
                                        <path fill-rule="evenodd" d="M7.646 10.854a.5.5 0 0 0 .708 0l2-2a.5.5 0 0 0-.708-.708L8.5 9.293V5.5a.5.5 0 0 0-1 0v3.793L6.354 8.146a.5.5 0 1 0-.708.708l2 2z"/>
                                        <path d="M4.406 3.342A5.53 5.53 0 0 1 8 2c2.69 0 4.923 2 5.166 4.579C14.758 6.804 16 8.137 16 9.773 16 11.569 14.502 13 12.687 13H3.781C1.708 13 0 11.366 0 9.318c0-1.763 1.266-3.223 2.942-3.593.143-.863.698-1.723 1.464-2.383zm.653.757c-.757.653-1.153 1.44-1.153 2.056v.448l-.445.049C2.064 6.805 1 7.952 1 9.318 1 10.785 2.23 12 3.781 12h8.906C13.98 12 15 10.988 15 9.773c0-1.216-1.02-2.228-2.313-2.228h-.5v-.5C12.188 4.825 10.328 3 8 3a4.53 4.53 0 0 0-2.941 1.1z"/>
                                    </svg>
                                </button>
                                <button
                                    class="btn btn-success btn-sm me-2"
                                    @click="addToFeeds(page)"
                                    :disabled="page.inFeeds || busyPageId !== null || !page.lastItemCount"
                                    :title="page.inFeeds ? language.get('This feed is already in your feeds') : language.get('Add this feed to your feeds')"
                                >
                                    <svg width="16" height="16" fill="currentColor" class="bi bi-rss" viewBox="0 0 16 16">
                                        <path d="M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2z"/>
                                        <path d="M5.5 12a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0m-3-8.5a1 1 0 0 1 1-1c5.523 0 10 4.477 10 10a1 1 0 1 1-2 0 8 8 0 0 0-8-8 1 1 0 0 1-1-1m0 4a1 1 0 0 1 1-1 6 6 0 0 1 6 6 1 1 0 1 1-2 0 4 4 0 0 0-4-4 1 1 0 0 1-1-1"/>
                                    </svg>
                                </button>
                                <button
                                    class="btn btn-warning btn-sm me-2"
                                    @click="editPage(page)"
                                    :disabled="busyPageId !== null"
                                    :title="language.get('Edit this page')"
                                >
                                    <svg width="16" height="16" fill="currentColor" class="bi bi-pencil" viewBox="0 0 16 16">
                                        <path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708L10.5 8.207l-3-3L12.146.146zM11.207 1.5L1.5 11.207V14.5h3.293L14.5 4.793 11.207 1.5z"/>
                                    </svg>
                                </button>
                                <button
                                    class="btn btn-danger btn-sm"
                                    @click="deletePage(page)"
                                    :disabled="busyPageId !== null"
                                    :title="language.get('Delete this page')"
                                >
                                    <svg width="16" height="16" fill="currentColor" class="bi bi-trash" viewBox="0 0 16 16">
                                        <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5Zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5Zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6Z"/>
                                        <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1ZM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118ZM2.5 3h11V2h-11v1Z"/>
                                    </svg>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div v-else-if="!loading" class="text-center text-muted mt-5">
                {{ language.get('No parsed pages yet. Add the URL of a page without a feed to make one.') }}
            </div>

            <div v-if="loading" class="loading-spinner-fixed">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        </div>
    </div>

    <ParserElementsModal
        :page="editingPage"
        :analysis="analysis"
        :show="showModal"
        @saved="handleSaved"
        @modal-hidden="handleModalHidden"
    />

    <ParserAddFeedModal
        :page="addingPage"
        :show="showAddFeedModal"
        @added="handleFeedAdded"
        @modal-hidden="showAddFeedModal = false"
    />

    <Error class="error-fixed-bottom" />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { language } from '@/functions/languageStore';
import { dateFormatStore } from '@/functions/dateFormatStore';
import { addFeedStore } from '@/functions/addFeedStore';
import config from '@/functions/config';
import Error from '@/components/error/Error.vue';
import ParserElementsModal from '@/components/parser/ParserElementsModal.vue';
import ParserAddFeedModal from '@/components/parser/ParserAddFeedModal.vue';
import { errorStore } from '@/components/error/errorStore';
import {
    analyzePage,
    deleteParsePage,
    getParsePages,
    parsePageNow
} from '@/api/parser';
import type { ParseAnalysisType, ParsePageType } from '@/types';

const url = ref('');
const pages = ref<ParsePageType[]>([]);
const loading = ref(false);
const analyzing = ref(false);
// The page whose action is running, or null
const busyPageId = ref<string | null>(null);

// Elements modal: an analyzed page to add, or a page to edit
const analysis = ref<ParseAnalysisType | null>(null);
const editingPage = ref<ParsePageType | null>(null);
const showModal = ref(false);

// Add feed modal: the page whose feed is added, in the chosen folder
const addingPage = ref<ParsePageType | null>(null);
const showAddFeedModal = ref(false);

/**
 * Message of a failed request; the parser answers with plain text, or { error } when analyzing
 */
const errorMessage = (error: any, fallback: string) => {
    const data = error?.response?.data;
    if (typeof data === 'string' && data) return data;
    return data?.error || fallback;
};

const fetchPages = async () => {
    try {
        loading.value = true;
        pages.value = await getParsePages();
    } catch (error: any) {
        errorStore.set(true, errorMessage(error, language.get('Failed to fetch the parsed pages')), error?.response?.status || 500);
    } finally {
        loading.value = false;
    }
};

/**
 * The generated feed, as the browser opens it
 */
const feedLink = (page: ParsePageType) => config.defaultServer() + page.feedPath;

const formatFrequency = (minutes: number) => {
    if (minutes % 1440 === 0) return `${minutes / 1440} ${language.get('d')}`;
    if (minutes % 60 === 0) return `${minutes / 60} ${language.get('h')}`;
    return `${minutes} ${language.get('min')}`;
};

/**
 * Reads the page; unless it blocks the access, opens the modal to choose the element of its posts
 */
const analyze = async () => {
    if (!url.value.trim() || analyzing.value) return;

    try {
        analyzing.value = true;
        analysis.value = await analyzePage(url.value.trim());
        editingPage.value = null;
        showModal.value = true;
    } catch (error: any) {
        errorStore.set(true, errorMessage(error, language.get('Failed to read the page')), error?.response?.status || 500);
    } finally {
        analyzing.value = false;
    }
};

const editPage = (page: ParsePageType) => {
    analysis.value = null;
    editingPage.value = page;
    showModal.value = true;
};

const handleSaved = (saved: ParsePageType) => {
    const index = pages.value.findIndex(page => page.id === saved.id);
    if (index >= 0) {
        pages.value[index] = saved;
    } else {
        pages.value.unshift(saved);
        url.value = '';
    }

    if (saved.lastError) {
        errorStore.set(true, saved.lastError, 400);
    } else {
        errorStore.set(true, `${language.get('Page parsed successfully')} (${saved.lastItemCount} ${language.get('posts')})`, 200);
    }
};

const handleModalHidden = () => {
    showModal.value = false;
};

const parseNow = async (page: ParsePageType) => {
    try {
        busyPageId.value = page.id;
        const parsed = await parsePageNow(page.id);
        handleSaved(parsed);
    } catch (error: any) {
        errorStore.set(true, errorMessage(error, language.get('Failed to parse the page')), error?.response?.status || 500);
    } finally {
        busyPageId.value = null;
    }
};

/**
 * Opens the modal to choose the folder of the page's feed
 */
const addToFeeds = (page: ParsePageType) => {
    addingPage.value = page;
    showAddFeedModal.value = true;
};

const handleFeedAdded = (page: ParsePageType, message: string) => {
    page.inFeeds = true;
    // Let the sidebar and the other pages refresh their feeds
    addFeedStore.addedCount.value++;
    errorStore.set(true, message, 200);
};

const deletePage = async (page: ParsePageType) => {
    const confirmed = confirm(
        `${language.get('Are you sure you want to delete this page? Its feed file is deleted too.')}\n\n${page.title}`
    );
    if (!confirmed) return;

    try {
        busyPageId.value = page.id;
        const result = await deleteParsePage(page.id);
        pages.value = pages.value.filter(item => item.id !== page.id);
        errorStore.set(true, result, 200);
    } catch (error: any) {
        errorStore.set(true, errorMessage(error, language.get('Failed to delete the page')), error?.response?.status || 500);
    } finally {
        busyPageId.value = null;
    }
};

onMounted(() => {
    fetchPages();
    document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
    document.body.style.overflow = 'auto';
});
</script>

<style scoped>
.scrollable-parser-view {
    height: 80vh;
    overflow-y: auto;
}

.parser-url-input {
    width: 450px;
}

.selector {
    display: inline-block;
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    vertical-align: middle;
}

.parser-actions-cell {
    white-space: nowrap;
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

/* On phones the URL input takes the full width and the table rows become stacked cards */
@media (max-width: 767.98px) {
    .parser-url-input {
        width: 100%;
    }

    .parser-toolbar > .btn {
        flex: 1 1 auto;
    }

    .parser-table thead {
        display: none;
    }

    .parser-table,
    .parser-table tbody,
    .parser-table tr,
    .parser-table td {
        display: block;
        width: 100%;
    }

    .parser-table tr {
        margin-bottom: 1rem;
        border: var(--bs-border-width) solid var(--bs-border-color);
        border-radius: var(--bs-border-radius);
        overflow: hidden;
    }

    .parser-table td {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 1rem;
        border: 0;
        text-align: right !important;
        overflow-wrap: anywhere;
    }

    .parser-table td[data-label]::before {
        content: attr(data-label);
        font-weight: 600;
        text-align: left;
        white-space: nowrap;
    }

    .parser-table td.parser-title-cell {
        justify-content: flex-start;
        flex-wrap: wrap;
        gap: 0.25rem;
        text-align: left !important;
        font-weight: 600;
        border-bottom: var(--bs-border-width) solid var(--bs-border-color);
    }

    .parser-table td.parser-actions-cell {
        justify-content: flex-end;
        gap: 0.5rem;
    }

    .parser-table td.parser-actions-cell .btn {
        margin: 0 !important;
    }
}

[data-bs-theme="dark"] .custom-table-header th {
    background-color: #343a40 !important;
    color: #ffffff !important;
    border-color: #495057 !important;
}
</style>
