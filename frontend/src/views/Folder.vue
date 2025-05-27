<template>
    <div class="folders-view">
        <ul v-if="folders.length">
            <li v-for="folder in folders" :key="folder.id">
                {{ folder.name }}
            </li>
        </ul>
        <div v-else>
            {{ language.get('No folders found') }}
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getFolders } from '@/api/folder';
import type { FolderType } from '@/types';
import { language } from '@/functions/languageStore';

const folders = ref<FolderType[]>([]);

onMounted(async () => {
    try {
        folders.value = await getFolders();
    } catch (error) {
        folders.value = [];
    }
});
</script>

<style scoped>
    .folders-view {
        padding: 2rem;
    }
</style>
