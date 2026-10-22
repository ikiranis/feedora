import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import * as bootstrap from "bootstrap"
import './style.scss'
import { themeStore } from './functions/themeStore'
import { authStore } from './functions/authStore'
import { initVideoControls } from './functions/videoPlayback'
import config from './functions/config'
import axios from 'axios'

// Expose Bootstrap globally so components can use its Modal API (and data-bs-dismiss works)
;(window as any).bootstrap = bootstrap

// Tells the Feedora Companion extension where the API is, so it can add the feed of a page from its popup
document.documentElement.dataset.feedoraServer = config.defaultServer()

// Initialize theme before creating the app
themeStore.initializeTheme()

// Tweet video players show their controls only on hover or click
initVideoControls()

// When the token expires while the app is open, the API answers 401: drop the session and show the login page
axios.interceptors.response.use(undefined, (error) => {
    const isLoginRequest = error.config?.url?.includes('/api/auth/login')

    if (error.response?.status === 401 && authStore.isAuthenticated.value && !isLoginRequest) {
        authStore.clearSession()
        router.push({ name: 'Auth' })
    }

    return Promise.reject(error)
})

// Initialize authentication and wait for it to complete
authStore.initialize().then(() => {
    const app = createApp(App)
    app.use(router)
    app.mount('#app')
})
