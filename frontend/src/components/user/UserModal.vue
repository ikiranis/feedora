<template>
    <!-- Add / Edit User Modal -->
    <div class="modal fade" id="userModal" ref="modalRef" tabindex="-1" aria-labelledby="userModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="userModalLabel">
                        {{ isEdit ? (language.get('Edit User') || 'Edit User') : (language.get('Add User') || 'Add User') }}
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form @submit.prevent="saveUser">
                        <div class="mb-3">
                            <label for="userUsernameInput" class="form-label">{{ language.get('Username') || 'Username' }}</label>
                            <input id="userUsernameInput" type="text" class="form-control" v-model="form.username" required />
                        </div>
                        <div class="mb-3">
                            <label for="userEmailInput" class="form-label">{{ language.get('Email') || 'Email' }}</label>
                            <input id="userEmailInput" type="email" class="form-control" v-model="form.email" required />
                        </div>
                        <div class="mb-3">
                            <label for="userPasswordInput" class="form-label">{{ language.get('Password') || 'Password' }}</label>
                            <input id="userPasswordInput" type="password" class="form-control" v-model="form.password"
                                autocomplete="new-password" :required="!isEdit" />
                            <div v-if="isEdit" class="form-text">
                                {{ language.get('Leave empty to keep the current password') || 'Leave empty to keep the current password' }}
                            </div>
                        </div>
                        <!-- Hidden submit so Enter saves the form -->
                        <button type="submit" class="d-none"></button>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                        {{ language.get('Cancel') || 'Cancel' }}
                    </button>
                    <button type="button" class="btn btn-primary" @click="saveUser" :disabled="!canSave || loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        {{ language.get('Save') || 'Save' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { addUser, updateUser } from '@/api/user';
import { language } from '@/functions/languageStore';
import { errorStore } from '@/components/error/errorStore';
import type { UserType, UserFormType } from '@/types';

const emit = defineEmits<{
    'user-saved': [];
}>();

const modalRef = ref<HTMLElement | null>(null);
// The user being edited, null when adding a new one
const editedUser = ref<UserType | null>(null);
const form = ref<UserFormType>({ username: '', email: '', password: '' });
const loading = ref(false);

const isEdit = computed(() => editedUser.value !== null);
const canSave = computed(() =>
    form.value.username.trim() !== '' && form.value.email.trim() !== '' && (isEdit.value || form.value.password !== '')
);

const modal = () => (window as any).bootstrap.Modal.getOrCreateInstance(modalRef.value);

/**
 * Opens the modal, for editing the given user or for adding a new one
 */
const open = (user: UserType | null = null) => {
    editedUser.value = user;
    form.value = { username: user?.username ?? '', email: user?.email ?? '', password: '' };
    modal().show();
};

const saveUser = async () => {
    if (!canSave.value || loading.value) return;

    try {
        loading.value = true;
        const result = editedUser.value
            ? await updateUser(editedUser.value.id, form.value)
            : await addUser(form.value);
        modal().hide();
        emit('user-saved');
        errorStore.set(true, result, 200);
    } catch (e: any) {
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to save user') || 'Failed to save user.', 500);
        }
    } finally {
        loading.value = false;
    }
};

defineExpose({ open });
</script>
