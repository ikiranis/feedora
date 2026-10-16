import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import * as bootstrap from "bootstrap"
import './style.scss'
import { themeStore } from './functions/themeStore'
import { authStore } from './functions/authStore'
import config from './functions/config'

// Expose Bootstrap globally so components can use its Modal API (and data-bs-dismiss works)
;(window as any).bootstrap = bootstrap

// Tells the Feedora Companion extension where the API is, so it can add the feed of a page from its popup
document.documentElement.dataset.feedoraServer = config.defaultServer()

// Initialize theme before creating the app
themeStore.initializeTheme()

// Initialize authentication and wait for it to complete
authStore.initialize().then(() => {
    const app = createApp(App)
    app.use(router)
    app.mount('#app')
})
