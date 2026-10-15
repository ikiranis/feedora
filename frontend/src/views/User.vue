<template>
    <div class="container py-3">
        <div class="row justify-content-center">
            <div class="col-auto">
                <div class="d-flex align-items-center gap-3">
                    <button
                        v-if="authStore.isAdmin.value"
                        class="btn btn-primary"
                        style="white-space: nowrap;"
                        @click="userModalRef?.open()"
                        :title="language.get('Add a new user') || 'Add a new user'"
                    >
                        {{ language.get('Add User') || 'Add User' }}
                    </button>
                    <div v-if="authStore.isAdmin.value" class="input-group" style="width: 250px;">
                        <span class="input-group-text">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-search" viewBox="0 0 16 16">
                                <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
                            </svg>
                        </span>
                        <input
                            v-model="searchTerm"
                            type="text"
                            class="form-control"
                            :placeholder="language.get('Search users...') || 'Search users...'"
                            :title="language.get('Filter users by username or email') || 'Filter users by username or email'"
                        />
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="scrollable-user-view">
        <div class="container py-4">
            <!-- Totals over all users -->
            <div v-if="users.length > 0 && authStore.isAdmin.value" class="row row-cols-2 row-cols-md-3 row-cols-xl-6 g-3 mb-4">
                <div class="col" v-for="stat in statCards" :key="stat.label">
                    <div class="card text-center h-100">
                        <div class="card-body py-2">
                            <div class="fs-4 fw-bold">{{ stat.value.toLocaleString() }}</div>
                            <div class="text-muted small">{{ stat.label }}</div>
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="filteredUsers.length > 0" class="table-responsive">
                <table class="table table-bordered table-hover align-middle">
                    <thead class="table-light custom-table-header">
                        <tr>
                            <th>{{ language.get('Username') || 'Username' }}</th>
                            <th>{{ language.get('Email') || 'Email' }}</th>
                            <th>{{ language.get('Role') || 'Role' }}</th>
                            <th class="text-end">{{ language.get('Folders') || 'Folders' }}</th>
                            <th class="text-end">{{ language.get('Feeds') || 'Feeds' }}</th>
                            <th class="text-end">{{ language.get('Posts') || 'Posts' }}</th>
                            <th class="text-end">{{ language.get('Unread') || 'Unread' }}</th>
                            <th class="text-end">{{ language.get('Saved') || 'Saved' }}</th>
                            <th class="text-end">{{ language.get('Logs') || 'Logs' }}</th>
                            <th>{{ language.get('Actions') || 'Actions' }}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="user in filteredUsers" :key="user.id">
                            <td>
                                <strong>{{ user.username }}</strong>
                                <span v-if="isCurrentUser(user)" class="badge bg-primary ms-2">{{ language.get('You') || 'You' }}</span>
                            </td>
                            <td>{{ user.email }}</td>
                            <td>
                                <select
                                    v-if="authStore.isAdmin.value && !isCurrentUser(user)"
                                    class="form-select form-select-sm"
                                    :value="user.role"
                                    @change="changeRole(user, ($event.target as HTMLSelectElement).value as Role)"
                                    :title="language.get('Change the role of this user') || 'Change the role of this user'"
                                >
                                    <option value="ADMIN">{{ roleLabel('ADMIN') }}</option>
                                    <option value="USER">{{ roleLabel('USER') }}</option>
                                </select>
                                <span v-else class="badge" :class="user.role === 'ADMIN' ? 'bg-danger' : 'bg-secondary'">
                                    {{ roleLabel(user.role) }}
                                </span>
                            </td>
                            <td class="text-end">{{ user.folderCount.toLocaleString() }}</td>
                            <td class="text-end">{{ user.feedCount.toLocaleString() }}</td>
                            <td class="text-end">{{ user.postCount.toLocaleString() }}</td>
                            <td class="text-end">{{ user.unreadCount.toLocaleString() }}</td>
                            <td class="text-end">{{ user.savedCount.toLocaleString() }}</td>
                            <td class="text-end">{{ user.logCount.toLocaleString() }}</td>
                            <td>
                                <div class="d-flex gap-2">
                                    <button
                                        class="btn btn-warning btn-sm"
                                        @click="userModalRef?.open(user)"
                                        :title="language.get('Edit this user') || 'Edit this user'"
                                    >
                                        <svg width="16" height="16" fill="currentColor" class="bi bi-pencil" viewBox="0 0 16 16">
                                            <path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708L10.5 8.207l-3-3L12.146.146zM11.207 1.5 1.5 11.207V14.5h3.293L14.5 4.793 11.207 1.5z"/>
                                        </svg>
                                    </button>
                                    <button
                                        v-if="authStore.isAdmin.value"
                                        class="btn btn-danger btn-sm"
                                        @click="deleteUser(user)"
                                        :disabled="isCurrentUser(user)"
                                        :title="isCurrentUser(user)
                                            ? (language.get(`You can't delete your own user`) || `You can't delete your own user`)
                                            : (language.get('Delete this user') || 'Delete this user')"
                                    >
                                        <svg width="16" height="16" fill="currentColor" class="bi bi-trash" viewBox="0 0 16 16">
                                            <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5Zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5Zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6Z"/>
                                            <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1ZM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118ZM2.5 3h11V2h-11v1Z"/>
                                        </svg>
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div v-else-if="!loading && users.length === 0" class="text-center text-muted mt-5">
                <h5>{{ language.get('No users found') || 'No users found' }}</h5>
            </div>
            <div v-else-if="!loading && searchTerm.trim()" class="text-center text-muted mt-5">
                <h5>{{ language.get('No users match your search') || 'No users match your search' }}</h5>
                <p class="text-muted">{{ language.get('Try adjusting your search terms') || 'Try adjusting your search terms' }}</p>
            </div>

            <div v-if="loading" class="loading-spinner-fixed">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">{{ language.get('Loading') || 'Loading' }}...</span>
                </div>
            </div>
        </div>
    </div>

    <UserModal ref="userModalRef" @user-saved="fetchUsers" />

    <Error class="error-fixed-bottom" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { getUsers, deleteUser as deleteUserApi, changeUserRole } from '@/api/user';
import type { Role, UserWithStatisticsType } from '@/types';
import { language } from '@/functions/languageStore';
import { authStore } from '@/functions/authStore';
import { errorStore } from '@/components/error/errorStore';
import Error from '@/components/error/Error.vue';
import UserModal from '@/components/user/UserModal.vue';

const users = ref<UserWithStatisticsType[]>([]);
const loading = ref(false);
const searchTerm = ref('');
const userModalRef = ref<InstanceType<typeof UserModal> | null>(null);

const filteredUsers = computed(() => {
    const term = searchTerm.value.trim().toLowerCase();
    if (!term) {
        return users.value;
    }
    return users.value.filter(user =>
        user.username.toLowerCase().includes(term) || user.email.toLowerCase().includes(term)
    );
});

const sum = (field: keyof UserWithStatisticsType) =>
    users.value.reduce((total, user) => total + (user[field] as number), 0);

const statCards = computed(() => [
    { label: language.get('Users') || 'Users', value: users.value.length },
    { label: language.get('Folders') || 'Folders', value: sum('folderCount') },
    { label: language.get('Feeds') || 'Feeds', value: sum('feedCount') },
    { label: language.get('Posts') || 'Posts', value: sum('postCount') },
    { label: language.get('Unread') || 'Unread', value: sum('unreadCount') },
    { label: language.get('Saved') || 'Saved', value: sum('savedCount') },
]);

const isCurrentUser = (user: UserWithStatisticsType) => authStore.user.value?.id === user.id;

const roleLabel = (role: Role) => role === 'ADMIN'
    ? (language.get('Admin') || 'Admin')
    : (language.get('User') || 'User');

/**
 * Changes the role of a user; only the admin can, and not for themselves
 */
const changeRole = async (user: UserWithStatisticsType, role: Role) => {
    try {
        loading.value = true;
        const result = await changeUserRole(user.id, role);
        errorStore.set(true, result, 200);
    } catch (e: any) {
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to save user') || 'Failed to save user.', 500);
        }
    } finally {
        // Reload, so a failed change doesn't stay selected
        await fetchUsers();
    }
};

const fetchUsers = async () => {
    try {
        loading.value = true;
        users.value = await getUsers();
    } catch (error) {
        users.value = [];
        errorStore.set(true, language.get('Failed to fetch users') || 'Failed to fetch users.', 500);
    } finally {
        loading.value = false;
    }
};

/**
 * Deletes a user, with all of their data, after confirmation
 */
const deleteUser = async (user: UserWithStatisticsType) => {
    const confirmed = confirm(
        `${language.get('Are you sure you want to delete this user') || 'Are you sure you want to delete this user'}?\n\n` +
        `"${user.username}" (${user.email})\n\n` +
        `${language.get('All of their folders, feeds, posts and logs will be deleted too') || 'All of their folders, feeds, posts and logs will be deleted too'}.`
    );
    if (!confirmed) {
        return;
    }

    try {
        loading.value = true;
        const result = await deleteUserApi(user.id);
        await fetchUsers();
        errorStore.set(true, result, 200);
    } catch (e: any) {
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to delete user') || 'Failed to delete user.', 500);
        }
    } finally {
        loading.value = false;
    }
};

onMounted(async () => {
    // Ensure body overflow is hidden when this component is active
    document.body.style.overflow = 'hidden';
    await fetchUsers();
});
</script>

<style scoped>
/* Make the main view scrollable instead of the whole page */
.scrollable-user-view {
    height: 80vh;
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

/* Custom table header for dark mode */
[data-bs-theme="dark"] .custom-table-header th {
    background-color: #343a40 !important;
    color: #ffffff !important;
    border-color: #495057 !important;
}
</style>
