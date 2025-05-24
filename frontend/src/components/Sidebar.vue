<script setup lang="ts">
import { language } from "@/functions/languageStore.ts";
import { computed, ComputedRef } from "vue";
import { useRoute } from "vue-router";
import LanguageSelect from "@/components/utilities/LanguageSelect.vue";

const route = useRoute()

const props = defineProps({
    collapsed: {
        type: Boolean,
        required: true
    }
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

    if (props.collapsed) {
        return classes + 'justify-content-center'
    }

    return classes + 'justify-content-start'
})

/**
 * Compute the icon size if collapsed or not
 */
const getIconSize: ComputedRef<number> = computed(() => {
    return props.collapsed ? 42 : 24
})
</script>

<template>
    <div>
        <div>
            <router-link :to="{ name: 'Home' }" title="Home" :class="menuItemClasses">
                <h2 class="d-flex">
                    <span>
                        <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize + 8" fill="white"
                            class="bi bi-house-door-fill my-auto" viewBox="0 0 16 16">
                            <path
                                d="M6.5 14.5v-3.505c0-.245.25-.495.5-.495h2c.25 0 .5.25.5.5v3.5a.5.5 0 0 0 .5.5h4a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293L8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h4a.5.5 0 0 0 .5-.5Z" />
                        </svg>
                    </span>

                    <span class="mx-3" v-if="!collapsed">feedora</span>
                </h2>
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
                <span class="mx-2" v-if="!collapsed">{{ language.get('Feeds') }}</span>
            </router-link>
            <router-link :to="{ name: 'Folders' }" :title="language.get('Folders')"
                :class="checkCurrentPage('Folders') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-folder" viewBox="0 0 16 16">
                        <path d="M9.828 4a.5.5 0 0 1 .354.146l.646.647H14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h4.172a.5.5 0 0 1 .354.146l1.328 1.328A.5.5 0 0 0 8.828 3H14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h4.172a.5.5 0 0 1 .354.146l1.328 1.328A.5.5 0 0 0 8.828 3H14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h4.172a.5.5 0 0 1 .354.146l1.328 1.328A.5.5 0 0 0 8.828 3z"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!collapsed">{{ language.get('Folders') }}</span>
            </router-link>
            <router-link :to="{ name: 'Users' }" :title="language.get('Users')"
                :class="checkCurrentPage('Users') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-person" viewBox="0 0 16 16">
                        <path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm4-3a4 4 0 1 1-8 0 4 4 0 0 1 8 0zM2 14s-1 0-1-1 1-4 7-4 7 3 7 4-1 1-1 1H2zm13-1c0-1-4-3-7-3s-7 2-7 3 4 1 7 1 7-0 7-1z"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!collapsed">{{ language.get('Users') }}</span>
            </router-link>
            <router-link :to="{ name: 'Posts' }" :title="language.get('Posts')"
                :class="checkCurrentPage('Posts') + ' ' + menuItemClasses">
                <span>
                    <svg xmlns="http://www.w3.org/2000/svg" :width="getIconSize" fill="currentColor" class="bi bi-file-earmark-text" viewBox="0 0 16 16">
                        <path d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5zm-3-.5a.5.5 0 0 1-.5-.5V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V5h-2.5A1.5 1.5 0 0 1 11 4z"/>
                        <path d="M5 7h6v1H5V7zm0 2h6v1H5V9zm0 2h4v1H5v-1z"/>
                    </svg>
                </span>
                <span class="mx-2" v-if="!collapsed">{{ language.get('Posts') }}</span>
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
                <span class="mx-2" v-if="!collapsed">{{ language.get("Settings") }}</span>
            </router-link>
        </div>

        <div class="mt-auto d-flex justify-content-center mb-3">
            <language-select :collapsed="collapsed" />
        </div>
    </div>
</template>
