import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import "bootstrap"
import './style.scss'
import { themeStore } from './functions/themeStore'
import { authStore } from './functions/authStore'

// Initialize theme before creating the app
themeStore.initializeTheme()

// Initialize authentication and wait for it to complete
authStore.initialize().then(() => {
    const app = createApp(App)
    app.use(router)
    app.mount('#app')
})
