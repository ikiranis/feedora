<script setup lang="ts">
import { language } from "@/functions/languageStore.ts";
import { computed, ComputedRef, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import LanguageSelect from "@/components/utilities/LanguageSelect.vue";
import { authStore } from "@/functions/authStore";
import { getAppVersion } from "@/api/general";
import { themeStore } from "@/functions/themeStore";

const route = useRoute()
const router = useRouter()

const props = defineProps({
    collapsed: {
        type: Boolean,
        required: true
    }
})

// Below the lg breakpoint the sidebar is a top bar, whose menu is opened with the hamburger button
const mobileQuery = window.matchMedia('(max-width: 991.98px)')
const isMobile = ref(mobileQuery.matches)
const menuOpen = ref(false)

const updateIsMobile = (event: MediaQueryListEvent) => {
    isMobile.value = event.matches
    if (!event.matches) menuOpen.value = false
}

// The mobile menu always shows the labels
const isCollapsed: ComputedRef<boolean> = computed(() => props.collapsed && !isMobile.value)

// Close the mobile menu when a page is chosen
watch(() => route.fullPath, () => {
    menuOpen.value = false
})

onUnmounted(() => {
    mobileQuery.removeEventListener('change', updateIsMobile)
})

/**
 * Check if the current page is the same as the given page
 *
 * @param page
 */
const checkCurrentPage = (page: string): string => {
    return route.name === page ? 'currentPage' : ''
}

/**
 * Compute the classes for the menu items
 */
const menuItemClasses: ComputedRef<string> = computed(() => {
    const classes = 'd-flex '

    if (isCollapsed.value) {
        return classes + 'justify-content-center'
    }

    return classes + 'justify-content-start'
})

/**
 * Compute the icon size if collapsed or not
 */
const getIconSize: ComputedRef<number> = computed(() => {
    return isCollapsed.value ? 22 : 20
})

const appVersion = ref<string | null>(null)

onMounted(async () => {
    mobileQuery.addEventListener('change', updateIsMobile)
    appVersion.value = await getAppVersion()
})

/**
 * Handle logout
 */
const handleLogout = async () => {
    try {
        await authStore.logout();
        router.push({ name: 'Auth' });
    } catch (error) {
        console.error('Logout error:', error);
        // Even if logout fails, redirect to auth
        router.push({ name: 'Auth' });
    }
}
</script>

<template>
    <div :class="{ 'menu-open': menuOpen }">
        <div class="sidebar-header d-flex align-items-center">
            <!-- The logo shows the posts of all folders -->
            <router-link :to="{ name: 'Posts', query: { all: '1' } }" title="Feedora" :class="'brand flex-grow-1 ' + menuItemClasses">
                <h2 class="d-flex m-0">
                    <span>
                        <img src="/feedora.svg?v=8" alt="Feedora" width="36" height="36"
                            class="my-auto" />
                    </span>

                    <span class="mx-3" v-if="!isCollapsed">feedora</span>
                </h2>
            </router-link>

            <!-- Mobile only: theme switch and the menu button -->
            <button type="button" class="sidebar-icon-button d-lg-none" @click="themeStore.toggleTheme"
                :title="themeStore.isDark() ? language.get('Light Mode') : language.get('Dark Mode')">
                <svg v-if="themeStore.isDark()" xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" class="bi bi-sun-fill" viewBox="0 0 16 16">
                    <path d="M8 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8M8 0a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-1 0v-2A.5.5 0 0 1 8 0m0 13a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-1 0v-2A.5.5 0 0 1 8 13m8-5a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1 0-1h2a.5.5 0 0 1 .5.5M3 8a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1 0-1h2A.5.5 0 0 1 3 8m10.657-5.657a.5.5 0 0 1 0 .707l-1.414 1.415a.5.5 0 1 1-.707-.708l1.414-1.414a.5.5 0 0 1 .707 0m-9.193 9.193a.5.5 0 0 1 0 .707L3.05 13.657a.5.5 0 0 1-.707-.707l1.414-1.414a.5.5 0 0 1 .707 0m9.193 2.121a.5.5 0 0 1-.707 0l-1.414-1.414a.5.5 0 0 1 .707-.707l1.414 1.414a.5.5 0 0 1 0 .707M4.464 4.465a.5.5 0 0 1-.707 0L2.343 3.05a.5.5 0 1 1 .707-.707l1.414 1.414a.5.5 0 0 1 0 .708"/>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-moon-fill" viewBox="0 0 16 16">
                    <path d="M6 .278a.77.77 0 0 1 .08.858 7.2 7.2 0 0 0-.878 3.46c0 4.021 3.278 7.277 7.318 7.277q.792-.001 1.533-.16a.79.79 0 0 1 .81.316.73.73 0 0 1-.031.893A8.35 8.35 0 0 1 8.344 16C3.734 16 0 12.286 0 7.71 0 4.266 2.114 1.312 5.124.06A.75.75 0 0 1 6 .278"/>
                </svg>
            </button>
            <button type="button" class="sidebar-icon-button d-lg-none" @click="menuOpen = !menuOpen"
                :aria-expanded="menuOpen" :title="menuOpen ? (language.get('Close menu') || 'Close menu') : (language.get('Open menu') || 'Open menu')">
                <svg v-if="menuOpen" xmlns="http://www.w3.org/2000/svg" width="28" height="28" fill="currentColor" class="bi bi-x-lg" viewBox="0 0 16 16">
                    <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="28" height="28" fill="currentColor" class="bi bi-list" viewBox="0 0 16 16">
                    <path fill-rule="evenodd" d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5"/>
                </svg>
            </button>
        </div>

        <div class="sidebar-menu">
            <router-link :to="{ name: 'Posts' }" :title="language.get('Posts')"
                :class="checkCurrentPage('Posts') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-file-earmark-text" viewBox="0 0 16 16">
                        <path d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5zm-3-.5a.5.5 0 0 1-.5-.5V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V5h-2.5A1.5 1.5 0 0 1 11 4z"/>
                        <path d="M5 7h6v1H5V7zm0 2h6v1H5V9zm0 2h4v1H5v-1z"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get('Posts') }}</span>
            </router-link>
            <router-link :to="{ name: 'LiveNewsfeed' }" :title="language.get('Live Newsfeed') || 'Live Newsfeed'"
                :class="checkCurrentPage('LiveNewsfeed') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-broadcast" viewBox="0 0 16 16">
                        <path d="M3.05 3.05a7 7 0 0 0 0 9.9.5.5 0 0 1-.707.707 8 8 0 0 1 0-11.314.5.5 0 0 1 .707.707m2.122 2.122a4 4 0 0 0 0 5.656.5.5 0 1 1-.708.708 5 5 0 0 1 0-7.072.5.5 0 0 1 .708.708m5.656-.708a.5.5 0 0 1 .708 0 5 5 0 0 1 0 7.072.5.5 0 1 1-.708-.708 4 4 0 0 0 0-5.656.5.5 0 0 1 0-.708m2.122-2.12a.5.5 0 0 1 .707 0 8 8 0 0 1 0 11.313.5.5 0 0 1-.707-.707 7 7 0 0 0 0-9.9.5.5 0 0 1 0-.707zM10 8a2 2 0 1 1-4 0 2 2 0 0 1 4 0"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get('Live Newsfeed') || 'Live Newsfeed' }}</span>
            </router-link>
            <router-link :to="{ name: 'Feeds' }" :title="language.get('Feeds')"
                :class="checkCurrentPage('Feeds') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-rss"
                        viewBox="0 0 16 16">
                        <path
                            d="M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2z" />
                        <path
                            d="M5.5 12a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0m-3-8.5a1 1 0 0 1 1-1c5.523 0 10 4.477 10 10a1 1 0 1 1-2 0 8 8 0 0 0-8-8 1 1 0 0 1-1-1m0 4a1 1 0 0 1 1-1 6 6 0 0 1 6 6 1 1 0 1 1-2 0 4 4 0 0 0-4-4 1 1 0 0 1-1-1" />
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get('Feeds') }}</span>
            </router-link>
            <router-link :to="{ name: 'Parser' }" :title="language.get('Parser')"
                :class="checkCurrentPage('Parser') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-file-earmark-code" viewBox="0 0 16 16">
                        <path d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5z"/>
                        <path d="M8.646 6.646a.5.5 0 0 1 .708 0l2 2a.5.5 0 0 1 0 .708l-2 2a.5.5 0 0 1-.708-.708L10.293 9 8.646 7.354a.5.5 0 0 1 0-.708m-1.292 0a.5.5 0 0 0-.708 0l-2 2a.5.5 0 0 0 0 .708l2 2a.5.5 0 0 0 .708-.708L5.707 9l1.647-1.646a.5.5 0 0 0 0-.708"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get('Parser') }}</span>
            </router-link>
            <router-link :to="{ name: 'Users' }" :title="language.get('Users')"
                :class="checkCurrentPage('Users') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-person" viewBox="0 0 16 16">
                        <path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm4-3a4 4 0 1 1-8 0 4 4 0 0 1 8 0zM2 14s-1 0-1-1 1-4 7-4 7 3 7 4-1 1-1 1H2zm13-1c0-1-4-3-7-3s-7 2-7 3 4 1 7 1 7-0 7-1z"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get('Users') }}</span>
            </router-link>
            <router-link :to="{ name: 'Logs' }" :title="language.get('Logs')"
                :class="checkCurrentPage('Logs') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-journal-text" viewBox="0 0 16 16">
                        <path d="M5 10.5a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 0 1h-2a.5.5 0 0 1-.5-.5m0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5"/>
                        <path d="M3 0h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-1h1v1a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v1H1V2a2 2 0 0 1 2-2"/>
                        <path d="M1 5v-.5a.5.5 0 0 1 1 0V5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1zm0 3v-.5a.5.5 0 0 1 1 0V8h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1zm0 3v-.5a.5.5 0 0 1 1 0v.5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1z"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get('Logs') }}</span>
            </router-link>
            <router-link :to="{ name: 'Settings' }" :class="checkCurrentPage('Settings') + ' ' + menuItemClasses"
                :title="language.get('Settings')">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor"
                        class="bi bi-gear-fill" viewBox="0 0 16 16">
                        <path
                            d="M9.405 1.05c-.413-1.4-2.397-1.4-2.81 0l-.1.34a1.464 1.464 0 0 1-2.105.872l-.31-.17c-1.283-.698-2.686.705-1.987 1.987l.169.311c.446.82.023 1.841-.872 2.105l-.34.1c-1.4.413-1.4 2.397 0 2.81l.34.1a1.464 1.464 0 0 1 .872 2.105l-.17.31c-.698 1.283.705 2.686 1.987 1.987l.311-.169a1.464 1.464 0 0 1 2.105.872l.1.34c.413 1.4 2.397 1.4 2.81 0l.1-.34a1.464 1.464 0 0 1 2.105-.872l.31.17c1.283.698 2.686-.705 1.987-1.987l-.169-.311a1.464 1.464 0 0 1 .872-2.105l.34-.1c1.4-.413 1.4-2.397 0-2.81l-.34-.1a1.464 1.464 0 0 1-.872-2.105l.17-.31c.698-1.283-.705-2.686-1.987-1.987l-.311.169a1.464 1.464 0 0 1-2.105-.872l-.1-.34zM8 10.93a2.929 2.929 0 1 1 0-5.86 2.929 2.929 0 0 1 0 5.858z" />
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get("Settings") }}</span>
            </router-link>
            <router-link :to="{ name: 'Support' }" :class="checkCurrentPage('Support') + ' ' + menuItemClasses"
                :title="language.get('Support')">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor"
                        class="bi bi-life-preserver" viewBox="0 0 16 16">
                        <path d="M8 15A7 7 0 1 0 8 1a7 7 0 0 0 0 14m0 1A8 8 0 1 1 8 0a8 8 0 0 1 0 16"/>
                        <path d="M8 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8m0 1a5 5 0 1 1 0-10 5 5 0 0 1 0 10"/>
                        <path d="M9.828 8.81 13.5 9.5l-.173.999-3.68-.708zM6.172 7.19 2.5 6.5l.173-.999 3.68.708zM7.19 6.172 6.5 2.5l.999-.173.708 3.68zM8.81 9.828l.708 3.68-.999.173-.708-3.68z"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get("Support") }}</span>
            </router-link>
            <router-link :to="{ name: 'Help' }" :class="checkCurrentPage('Help') + ' ' + menuItemClasses"
                :title="language.get('Help')">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor"
                        class="bi bi-question-circle-fill" viewBox="0 0 16 16">
                        <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0M5.496 6.033h.825c.138 0 .248-.113.266-.25.09-.656.54-1.134 1.342-1.134.686 0 1.314.343 1.314 1.168 0 .635-.374.927-.965 1.371-.673.489-1.206 1.06-1.168 1.987l.003.217a.25.25 0 0 0 .25.246h.811a.25.25 0 0 0 .25-.25v-.105c0-.718.273-.927 1.01-1.486.609-.463 1.244-.977 1.244-2.056 0-1.511-1.276-2.241-2.673-2.241-1.267 0-2.655.59-2.75 2.286a.237.237 0 0 0 .241.247m2.325 6.443c.61 0 1.029-.394 1.029-.927 0-.552-.42-.94-1.029-.94-.584 0-1.009.388-1.009.94 0 .533.425.927 1.01.927z"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!isCollapsed">{{ language.get("Help") }}</span>
            </router-link>
        </div>

        <div class="sidebar-menu mt-auto">
            <!-- Version of the running app -->
            <div v-if="appVersion" class="app-version text-center mb-2" :title="'Feedora ' + appVersion">
                {{ isCollapsed ? appVersion : 'Feedora v' + appVersion }}
            </div>

            <!-- User Info and Logout -->
            <div v-if="authStore.user.value" class="user-section mb-2">
                <div v-if="!isCollapsed" class="user-info rounded p-2 mb-2">
                    <div class="d-flex align-items-center mb-2">
                        <div class="user-avatar me-2">
                            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" class="bi bi-person-circle" viewBox="0 0 16 16">
                                <path d="M11 6a3 3 0 1 1-6 0 3 3 0 0 1 6 0z"/>
                                <path fill-rule="evenodd" d="M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8zm8-7a7 7 0 0 0-5.468 11.37C3.242 11.226 4.805 10 8 10s4.757 1.225 5.468 2.37A7 7 0 0 0 8 1z"/>
                            </svg>
                        </div>
                        <div class="user-details flex-grow-1">
                            <div class="username fw-semibold text-truncate">{{ authStore.user.value.username }}</div>
                            <div class="email text-muted small text-truncate">{{ authStore.user.value.email }}</div>
                        </div>
                    </div>
                </div>
                
                <button 
                    @click="handleLogout" 
                    :class="menuItemClasses + ' logout-btn'"
                    :title="language.get('Logout')"
                >
                    <span>
                        <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-box-arrow-right" viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M10 12.5a.5.5 0 0 1-.5.5h-8a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5h8a.5.5 0 0 1 .5.5v2a.5.5 0 0 0 1 0v-2A1.5 1.5 0 0 0 9.5 2h-8A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h8a1.5 1.5 0 0 0 1.5-1.5v-2a.5.5 0 0 0-1 0v2z"/>
                            <path fill-rule="evenodd" d="m15.854 8.354-3-3a.5.5 0 0 0-.708.708L14.293 7.5H5.5a.5.5 0 0 0 0 1h8.793l-2.147 2.146a.5.5 0 0 0 .708.708l3-3a.5.5 0 0 0 0-.708z"/>
                        </svg>
                    </span>
                    <span class="mx-2" v-if="!isCollapsed">{{ language.get('Logout') }}</span>
                </button>
            </div>
            
            <!-- Language Selector -->
            <div class="d-flex justify-content-center mb-3">
                <language-select :collapsed="isCollapsed" />
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.app-version {
    font-size: 0.75rem;
    color: #adb5bd;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.user-section {
    .user-info {
        background-color: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
    }
    
    .user-avatar {
        color: #f8f9fa;
    }
    
    .username {
        font-size: 0.9rem;
        max-width: 120px;
        color: #f8f9fa;
    }
    
    .email {
        font-size: 0.75rem;
        max-width: 120px;
        color: #adb5bd !important;
    }
}

[data-bs-theme="dark"] {
    .user-section {
        // Same styling for both light and dark modes since sidebar is always dark
        .user-info {
            background-color: rgba(255, 255, 255, 0.1);
            border: 1px solid rgba(255, 255, 255, 0.2);
        }
        
        .user-avatar {
            color: #f8f9fa;
        }
        
        .username {
            color: #f8f9fa;
        }
        
        .email {
            color: #adb5bd !important;
        }
    }
}
</style>
