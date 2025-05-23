<script setup lang="ts">
import { language } from "@/functions/languageStore.ts";
</script>

<template>
    <div class="container-fluid mt-5 text-center">
        <div class="d-lg-flex flex-lg-wrap justify-content-center">
            <router-link class="btn p-0 m-3" :to="{ name: 'Feeds' }">
                <div class="card">
                    <div class="card-img-top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor" class="bi bi-rss" viewBox="0 0 16 16">
                            <path d="M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2z" />
                            <path d="M5.5 12a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0m-3-8.5a1 1 0 0 1 1-1c5.523 0 10 4.477 10 10a1 1 0 1 1-2 0 8 8 0 0 0-8-8 1 1 0 0 1-1-1m0 4a1 1 0 0 1 1-1 6 6 0 0 1 6 6 1 1 0 1 1-2 0 4 4 0 0 0-4-4 1 1 0 0 1-1-1" />
                        </svg>
                    </div>
                    <div class="card-body">
                        <h5 class="card-title">{{ language.get('Feeds') }}</h5>
                        <p class="card-text">{{ language.get('List of RSS feeds') }}</p>
                    </div>
                </div>
            </router-link>
            <router-link class="btn p-0 m-3" :to="{ name: 'Folders' }">
                <div class="card">
                    <div class="card-img-top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor" class="bi bi-folder" viewBox="0 0 16 16">
                            <path d="M9.828 4a.5.5 0 0 1 .354.146l.646.647H14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h4.172a.5.5 0 0 1 .354.146l1.328 1.328A.5.5 0 0 0 8.828 3H14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h4.172a.5.5 0 0 1 .354.146l1.328 1.328A.5.5 0 0 0 8.828 3z"/>
                        </svg>
                    </div>
                    <div class="card-body">
                        <h5 class="card-title">{{ language.get('Folders') }}</h5>
                        <p class="card-text">{{ language.get('List of folders') }}</p>
                    </div>
                </div>
            </router-link>
            <router-link class="btn p-0 m-3" :to="{ name: 'Users' }">
                <div class="card">
                    <div class="card-img-top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor" class="bi bi-person" viewBox="0 0 16 16">
                            <path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm4-3a4 4 0 1 1-8 0 4 4 0 0 1 8 0zM2 14s-1 0-1-1 1-4 7-4 7 3 7 4-1 1-1 1H2zm13-1c0-1-4-3-7-3s-7 2-7 3 4 1 7 1 7-0 7-1z"/>
                        </svg>
                    </div>
                    <div class="card-body">
                        <h5 class="card-title">{{ language.get('Users') }}</h5>
                        <p class="card-text">{{ language.get('List of users') }}</p>
                    </div>
                </div>
            </router-link>
            <router-link class="btn p-0 m-3" :to="{ name: 'Settings' }">
                <div class="card">
                    <div class="card-img-top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor"
                            class="bi bi-gear-fill" viewBox="0 0 16 16">
                            <path
                                d="M9.405 1.05c-.413-1.4-2.397-1.4-2.81 0l-.1.34a1.464 1.464 0 0 1-2.105.872l-.31-.17c-1.283-.698-2.686.705-1.987 1.987l.169.311c.446.82.023 1.841-.872 2.105l-.34.1c-1.4.413-1.4 2.397 0 2.81l.34.1a1.464 1.464 0 0 1 .872 2.105l-.17.31c-.698 1.283.705 2.686 1.987 1.987l.311-.169a1.464 1.464 0 0 1 2.105.872l.1.34c.413 1.4 2.397 1.4 2.81 0l.1-.34a1.464 1.464 0 0 1 2.105-.872l.31.17c1.283.698 2.686-.705 1.987-1.987l-.169-.311a1.464 1.464 0 0 1 .872-2.105l.34-.1c1.4-.413 1.4-2.397 0-2.81l-.34-.1a1.464 1.464 0 0 1-.872-2.105l.17-.31c.698-1.283-.705-2.686-1.987-1.987l-.311.169a1.464 1.464 0 0 1-2.105-.872l-.1-.34zM8 10.93a2.929 2.929 0 1 1 0-5.86 2.929 2.929 0 0 1 0 5.858z" />
                        </svg>
                    </div>

                    <div class="card-body">
                        <h5 class="card-title">{{ language.get("Settings") }}</h5>

                        <p class="card-text">{{ language.get("App Settings") }}</p>
                    </div>
                </div>
            </router-link>
        </div>
    </div>
</template>

<style scoped lang="scss">
.logo {
    height: 6em;
    padding: 1.5em;
    will-change: filter;
    transition: filter 300ms;
}

.logo:hover {
    filter: drop-shadow(0 0 2em #646cffaa);
}

.logo.vue:hover {
    filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
