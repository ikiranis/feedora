<template>
    <Teleport to="body">
        <div class="modal fade" id="parserElementsModal" tabindex="-1" aria-labelledby="parserElementsModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg modal-dialog-scrollable">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="parserElementsModalLabel">
                            {{ page ? language.get('Edit parsed page') : language.get('Choose the posts of the page') }}
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <form @submit.prevent="save">
                            <div class="mb-3">
                                <label class="form-label">{{ language.get('Page URL') }}</label>
                                <div class="form-control-plaintext text-break py-0">
                                    <a :href="pageUrl" target="_blank" rel="noopener" class="text-decoration-none">{{ pageUrl }}</a>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label for="parserFeedTitle" class="form-label">{{ language.get('Feed Title') }}</label>
                                <input id="parserFeedTitle" type="text" class="form-control" v-model="title" required />
                            </div>

                            <div class="mb-3">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <label class="form-label mb-0">{{ language.get('Element of the posts') }}</label>
                                    <button
                                        v-if="page"
                                        type="button"
                                        class="btn btn-outline-secondary btn-sm"
                                        @click="detectElements"
                                        :disabled="detecting"
                                    >
                                        <span v-if="detecting" class="spinner-border spinner-border-sm me-1" role="status"></span>
                                        {{ language.get('Detect elements') }}
                                    </button>
                                </div>

                                <div v-if="candidates.length > 0" class="list-group mb-2">
                                    <label
                                        v-for="candidate in candidates"
                                        :key="candidate.selector"
                                        class="list-group-item list-group-item-action candidate"
                                        :class="{ active: selector === candidate.selector }"
                                    >
                                        <div class="d-flex align-items-start gap-2">
                                            <input
                                                class="form-check-input flex-shrink-0 mt-1"
                                                type="radio"
                                                name="parserCandidate"
                                                :value="candidate.selector"
                                                v-model="selector"
                                            />
                                            <div class="flex-grow-1 min-w-0">
                                                <div class="d-flex flex-wrap justify-content-between gap-2">
                                                    <code class="text-break">{{ candidate.selector }}</code>
                                                    <span class="badge text-bg-secondary">{{ candidate.count }} {{ language.get('posts') }}</span>
                                                </div>
                                                <ul class="small mb-0 mt-1 ps-3 samples">
                                                    <li v-for="sample in candidate.samples" :key="sample.link" class="text-truncate">
                                                        {{ sample.title }}
                                                    </li>
                                                </ul>
                                            </div>
                                            <img
                                                v-if="candidate.samples[0]?.image"
                                                :src="candidate.samples[0].image"
                                                alt=""
                                                class="candidate-image rounded flex-shrink-0"
                                                @error="($event.target as HTMLImageElement).style.display = 'none'"
                                            />
                                        </div>
                                    </label>
                                </div>

                                <label for="parserSelector" class="form-label small text-muted mb-1">{{ language.get('CSS selector') }}</label>
                                <input id="parserSelector" type="text" class="form-control font-monospace" v-model="selector" required />
                            </div>

                            <div class="mb-3">
                                <label for="parserFrequency" class="form-label">{{ language.get('Parse every (minutes)') }}</label>
                                <input
                                    id="parserFrequency"
                                    type="number"
                                    class="form-control frequency-input"
                                    v-model.number="frequency"
                                    :min="MIN_FREQUENCY"
                                    :max="MAX_FREQUENCY"
                                    required
                                />
                            </div>

                            <div v-if="errorMessage" class="alert alert-danger mb-0">{{ errorMessage }}</div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            {{ language.get('Close') }}
                        </button>
                        <button type="button" class="btn btn-primary" @click="save" :disabled="!canSave || saving">
                            <span v-if="saving" class="spinner-border spinner-border-sm me-2" role="status"></span>
                            {{ language.get('Save') }}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { language } from '@/functions/languageStore';
import { addParsePage, analyzePage, updateParsePage } from '@/api/parser';
import type { ParseAnalysisType, ParseCandidateType, ParsePageType } from '@/types';

const DEFAULT_FREQUENCY = 60;
const MIN_FREQUENCY = 5;
const MAX_FREQUENCY = 10080;

interface Props {
    // The page to edit, null when adding a new one
    page?: ParsePageType | null;
    // The analyzed page with its candidate elements, when adding a new one
    analysis?: ParseAnalysisType | null;
    show?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    page: null,
    analysis: null,
    show: false
});

const emit = defineEmits<{
    'saved': [page: ParsePageType];
    'modal-hidden': [];
}>();

const title = ref('');
const selector = ref('');
const frequency = ref(DEFAULT_FREQUENCY);
const candidates = ref<ParseCandidateType[]>([]);
const saving = ref(false);
const detecting = ref(false);
const errorMessage = ref('');

const pageUrl = computed(() => props.page?.url ?? props.analysis?.url ?? '');

const canSave = computed(() => title.value.trim() !== '' && selector.value.trim() !== ''
    && frequency.value >= MIN_FREQUENCY && frequency.value <= MAX_FREQUENCY);

const modalElement = () => document.getElementById('parserElementsModal');

/**
 * Fills the form with the edited page, or with the most likely candidate of the analyzed page
 */
const resetForm = () => {
    errorMessage.value = '';
    if (props.page) {
        title.value = props.page.title;
        selector.value = props.page.selector;
        frequency.value = props.page.frequency;
        candidates.value = [];
    } else if (props.analysis) {
        title.value = props.analysis.title;
        candidates.value = props.analysis.candidates;
        selector.value = candidates.value[0]?.selector ?? '';
        frequency.value = DEFAULT_FREQUENCY;
    }
};

watch(() => props.show, (show) => {
    const element = modalElement();
    if (!element) return;

    if (show) {
        resetForm();
        (window as any).bootstrap.Modal.getOrCreateInstance(element).show();
    } else {
        (window as any).bootstrap.Modal.getInstance(element)?.hide();
    }
});

const handleHidden = () => emit('modal-hidden');

onMounted(() => modalElement()?.addEventListener('hidden.bs.modal', handleHidden));

onUnmounted(() => {
    const element = modalElement();
    element?.removeEventListener('hidden.bs.modal', handleHidden);
    (window as any).bootstrap.Modal.getInstance(element)?.dispose();
});

/**
 * Finds the repeated elements of the edited page again, e.g. after the site changed its layout
 */
const detectElements = async () => {
    if (!props.page) return;

    try {
        detecting.value = true;
        errorMessage.value = '';
        const analysis = await analyzePage(props.page.url);
        candidates.value = analysis.candidates;
    } catch (error: any) {
        errorMessage.value = error.response?.data?.error || language.get('Failed to read the page');
    } finally {
        detecting.value = false;
    }
};

/**
 * Adds the analyzed page, or saves the edited one. Both are parsed right away by the server.
 */
const save = async () => {
    if (!canSave.value || saving.value) return;

    const request = {
        title: title.value.trim(),
        selector: selector.value.trim(),
        frequency: frequency.value
    };

    try {
        saving.value = true;
        errorMessage.value = '';
        const saved = props.page
            ? await updateParsePage(props.page.id, request)
            : await addParsePage({ ...request, url: props.analysis?.url });
        emit('saved', saved);
        (window as any).bootstrap.Modal.getInstance(modalElement())?.hide();
    } catch (error: any) {
        errorMessage.value = typeof error.response?.data === 'string'
            ? error.response.data
            : language.get('Failed to save the page');
    } finally {
        saving.value = false;
    }
};
</script>

<style scoped>
.candidate {
    cursor: pointer;
}

.candidate.active code {
    color: inherit;
}

.candidate-image {
    width: 64px;
    height: 48px;
    object-fit: cover;
}

.samples {
    opacity: 0.85;
}

.min-w-0 {
    min-width: 0;
}

.frequency-input {
    max-width: 160px;
}
</style>
