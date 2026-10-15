<script setup lang="ts">
import config from "@/functions/config.ts"
import {language} from "@/functions/languageStore.ts"
import {setNewLanguage} from "@/api/language.ts";

defineProps({
    collapsed: {
        type: Boolean,
        required: false
    }
})

const setLanguage = async (lang: string) => {
    config.setLanguage(lang)
    await language.init()

    await setNewLanguage({language: lang})
}

/**
 * Create flag path
 *
 * @param language
 */
const getFlagPath = (language: string) => {
    const basePath = '/flags/'

    return basePath + language + '.svg'
}

</script>

<template>
    <div class="d-flex gap-2" :class="collapsed ? 'flex-column align-items-center' : ''">
        <a v-for="language in config.getLanguages()" class="flag" role="button"
           :class="config.defaultLanguage() === language.lang ? 'currentLanguage' : ''"
           @click="setLanguage(language.lang)">
            <img :src="getFlagPath(language.lang)" :alt="language.name"
                 :title="language.name" width="28" height="20">
        </a>
    </div>
</template>

<style scoped lang="scss">
    .flag {
        display: flex;
        padding: 3px;
        border-radius: 6px;
        border: 2px solid transparent;
        opacity: 0.5;
        cursor: pointer;
        transition: opacity 0.2s, border-color 0.2s;

        img {
            border-radius: 3px;
            object-fit: cover;
        }

        &:hover {
            opacity: 0.85;
        }
    }

    .currentLanguage {
        opacity: 1;
        border-color: rgba(255, 255, 255, 0.6);
    }
</style>
