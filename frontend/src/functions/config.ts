import {ref, Ref} from "vue";
import {Language} from "@/types";

const language: Ref<string> = ref('')
const languages: Array<Language> = [
        {
            lang: 'el',
            name: 'Ελληνικά'
        },
        {
            lang: 'en',
            name: 'English'
        }
    ]

/**
 * The backend URL. In the production build the app is served by the backend itself, so the API is on
 * the same origin (works with any host/IP/port, e.g. behind Docker). The Vite dev server needs the full URL.
 */
const defaultServer = () => {
    return import.meta.env.DEV ? 'http://localhost:9990' : window.location.origin
}

/**
 * Get the default language from local storage
 */
const defaultLanguage = () => {
    if (language.value === '') {
        const defaultLang = localStorage.getItem('defaultLanguage')

        if (defaultLang) {
            language.value = defaultLang
        } else {
            language.value = 'el'
        }
    }

    return language.value
}

/**
 * Set the default language and save it to local storage
 *
 * @param lang
 */
const setLanguage = (lang: string) => {
    language.value = lang
    localStorage.setItem('defaultLanguage', lang)
}

const getLanguages = () => {
    return languages
}

export default {
    defaultServer,
    defaultLanguage,
    setLanguage,
    getLanguages
}
