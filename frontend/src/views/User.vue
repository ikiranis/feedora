<template>
    <div class="users-view">
        <ul v-if="users.length">
            <li v-for="user in users" :key="user.id">
                {{ user.username }} ({{ user.email }})
            </li>
        </ul>
        <div v-else>
            No users found.
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getUsers } from '@/api/user';
import type { UserType } from '@/types';

const users = ref<UserType[]>([]);

onMounted(async () => {
    try {
        users.value = await getUsers();
    } catch (error) {
        users.value = [];
    }
});
</script>

<style scoped>
    .users-view {
        padding: 2rem;
    }
</style>
