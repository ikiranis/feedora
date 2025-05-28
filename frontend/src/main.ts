import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import "bootstrap"
import './style.scss'
import { themeStore } from './functions/themeStore'

// Initialize theme before creating the app
themeStore.initializeTheme()

const app = createApp(App)
app.use(router)
app.mount('#app')
