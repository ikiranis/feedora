<script setup lang="ts">
import {language} from "./functions/languageStore.ts";
import {onMounted, ref} from "vue";
import {checkAppAlive} from "@/api/general.ts";
import Sidebar from "@/components/Sidebar.vue";
import Page404 from "@/components/error/Page404.vue";
import { themeStore } from "./functions/themeStore.ts";
import { authStore } from "./functions/authStore.ts";

const collapsed = ref(false)
const isAppAlive = ref(true)

onMounted(() => {
    // Initial call
    updateAppStatus();

    // Run the update every 10 seconds
    setInterval(updateAppStatus, 60000);
})

/**
 * Check if the app is alive
 */
const updateAppStatus = async () => {
    try {
        const response = await checkAppAlive();

        if (response) {
            isAppAlive.value = true; // Response is a boolean

            return
        }

        isAppAlive.value = false; // Response is not a boolean
    } catch (error) {
        isAppAlive.value = false; // Handle any errors and return false
    }
};

/**
 * Toggle collapse for sidebar
 */
const collapse = () => {
    const sidebar = document.querySelector('.sidebar')
    const content = document.querySelector('.content')

    if (sidebar && content) {
        sidebar.classList.toggle('col-lg-1')
        sidebar.classList.toggle('col-lg-2')
        content.classList.toggle('col-lg-10')
        content.classList.toggle('col-lg-11')

        collapsed.value = !collapsed.value
    }
}

</script>

<template>
    <!-- Show main app when server is alive -->
    <div v-if="isAppAlive">
        <!-- Authenticated user layout with sidebar -->
        <div v-if="authStore.isAuthenticated.value" class="row vh-100">
            <sidebar class="sidebar d-flex flex-column col-lg-2 col-12" :collapsed="collapsed"/>

            <div class="content col-lg-10 col-12 px-5">
                <div class="d-none d-lg-block d-flex">
                    <span @click="collapse" class="collapseButton btn" :title="collapsed ? language.get('Expand') : language.get('Collapse')">
                        <span v-if="collapsed">
                            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" class="bi bi-arrow-right-circle" viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M1 8a7 7 0 1 0 14 0A7 7 0 0 0 1 8zm15 0A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM4.5 7.5a.5.5 0 0 0 0 1h5.793l-2.147 2.146a.5.5 0 0 0 .708.708l3-3a.5.5 0 0 0 0-.708l-3-3a.5.5 0 1 0-.708.708L10.293 7.5H4.5z"/>
                            </svg>
                        </span>
                        <span v-else>
                            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" class="bi bi-arrow-left-circle" viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M1 8a7 7 0 1 0 14 0A7 7 0 0 0 1 8zm15 0A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-4.5-.5a.5.5 0 0 1 0 1H5.707l2.147 2.146a.5.5 0 0 1-.708.708l-3-3a.5.5 0 0 1 0-.708l3-3a.5.5 0 1 1 .708.708L5.707 7.5H11.5z"/>
                             </svg>
                        </span>
                    </span>
                    
                    <span @click="themeStore.toggleTheme" class="themeButton btn ms-2" 
                          :title="themeStore.isDark() ? language.get('Light Mode') : language.get('Dark Mode')">
                        <span v-if="themeStore.isDark()">
                            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" class="bi bi-sun-fill" viewBox="0 0 16 16">
                                <path d="M8 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM8 0a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-1 0v-2A.5.5 0 0 1 8 0zm0 13a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-1 0v-2A.5.5 0 0 1 8 13zm8-5a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1 0-1h2a.5.5 0 0 1 .5.5zM3 8a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1 0-1h2A.5.5 0 0 1 3 8zm10.657-5.657a.5.5 0 0 1 0 .707l-1.414 1.415a.5.5 0 1 1-.707-.708l1.414-1.414a.5.5 0 0 1 .707 0zm-9.193 9.193a.5.5 0 0 1 0 .707L3.05 13.657a.5.5 0 0 1-.707-.707l1.414-1.414a.5.5 0 0 1 .707 0zm9.193 2.121a.5.5 0 0 1-.707 0l-1.414-1.414a.5.5 0 0 1 .707-.707l1.414 1.414a.5.5 0 0 1 0 .707zM4.464 4.465a.5.5 0 0 1-.707 0L2.343 3.05a.5.5 0 1 1 .707-.707l1.414 1.414a.5.5 0 0 1 0 .708z"/>
                            </svg>
                        </span>
                        <span v-else>
                            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" class="bi bi-moon-fill" viewBox="0 0 16 16">
                                <path d="M6 .278a.768.768 0 0 1 .08.858 7.208 7.208 0 0 0-.878 3.46c0 4.021 3.278 7.277 7.318 7.277.527 0 1.04-.055 1.533-.16a.787.787 0 0 1 .81.316.733.733 0 0 1-.031.893A8.349 8.349 0 0 1 8.344 16C3.734 16 0 12.286 0 7.71 0 4.266 2.114 1.312 5.124.06A.752.752 0 0 1 6 .278z"/>
                            </svg>
                        </span>
                    </span>
                </div>

                <router-view/>
            </div>
        </div>
        
        <!-- Unauthenticated user layout - full screen router view -->
        <div v-else class="vh-100">
            <router-view/>
        </div>
    </div>

    <!-- Server offline page -->
    <Page404 v-else
             :title="language.get('Server Is Offline')"
             :text="language.get('Can\'t connect to the API service')" />
</template>

<style scoped lang="scss">
.collapseButton {
    position: relative;
    left: -3em;
    //opacity: 0.3; /* Set the default opacity, where 1.0 is fully opaque and 0.0 is fully transparent */
    transition: color 0.3s; /* Add a smooth transition effect for opacity changes */
    color: lightgrey;

    &:hover {
        //opacity: 1.0; /* Set opacity to fully opaque on hover */
        color: black;
    }
}

.themeButton {
    position: relative;
    left: -3em;
    transition: color 0.3s;
    color: lightgrey;

    &:hover {
        color: black;
    }
}

[data-bs-theme="dark"] {
    .collapseButton:hover,
    .themeButton:hover {
        color: white;
    }
}
</style>
