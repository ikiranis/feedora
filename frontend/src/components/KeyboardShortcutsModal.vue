<template>
    <Teleport to="body">
        <div class="modal fade" ref="modalRef" tabindex="-1" aria-labelledby="keyboardShortcutsModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-scrollable">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="keyboardShortcutsModalLabel">
                            {{ language.get('Keyboard shortcuts') || 'Keyboard shortcuts' }}
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <table class="table table-sm mb-0 shortcuts-table">
                            <tbody>
                                <tr v-for="shortcut in shortcuts" :key="shortcut.description">
                                    <th scope="row" class="fw-normal">
                                        <template v-for="(key, index) in shortcut.keys" :key="key">
                                            <span v-if="index > 0" class="text-muted mx-1">/</span>
                                            <kbd>{{ key }}</kbd>
                                        </template>
                                    </th>
                                    <td>{{ language.get(shortcut.description) || shortcut.description }}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            {{ language.get('Close') || 'Close' }}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { language } from '@/functions/languageStore';

interface Props {
    show?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    show: false
});

const emit = defineEmits<{
    'modal-hidden': [];
}>();

const shortcuts: { keys: string[], description: string }[] = [
    { keys: ['←', '→'], description: 'Select the previous / next post' },
    { keys: ['↑', '↓'], description: 'Select the post above / below, or scroll in list mode and in the opened post' },
    { keys: ['Enter', 'O'], description: 'Open / close the selected post' },
    { keys: ['Esc'], description: 'Close the opened post' },
    { keys: ['B'], description: 'Open the link of the selected post in a background tab' },
    { keys: ['F'], description: 'Save the selected post for later / unsave it' },
    { keys: ['S'], description: 'Add the selected post to the favorites / remove it' },
    { keys: ['C'], description: 'Copy the link of the selected post to the clipboard' },
    { keys: ['W'], description: 'Show / hide the article of the link preview inside the selected post' },
    { keys: ['H'], description: 'Show / hide the keyboard shortcuts' },
];

const modalRef = ref<HTMLElement | null>(null);

const getModal = () => (window as any).bootstrap.Modal.getOrCreateInstance(modalRef.value);

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
.shortcuts-table th {
    width: 35%;
    white-space: nowrap;
}

.shortcuts-table td {
    vertical-align: middle;
}
</style>
