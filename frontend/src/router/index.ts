// You have to add at include declaration the "router/**/*.ts", on tsconfig.json
// and install vue-router with npm

import {RouteRecordRaw, createRouter, createWebHashHistory} from "vue-router"
import Home from "@/views/Home.vue"
import Settings from "@/views/setting/Settings.vue"
import Setting from "@/views/setting/Setting.vue"
import Feed from "@/views/Feed.vue"
import Folder from "@/views/Folder.vue"

const routes : RouteRecordRaw[] = [
    {
        path: '/',
        name: 'Home',
        component: Home
    },
    {
        path: '/feeds',
        name: 'Feeds',
        component: Feed
    },
    {
        path: '/settings',
        name: 'Settings',
        component: Settings
    },
    {
        path: '/setting/:id',
        name: 'Setting',
        component: Setting,
        props: true
    },
    {
        path: '/folders',
        name: 'Folders',
        component: Folder
    },
]

// Set route path with webHistory, on current path
const router = createRouter({
    // history: createWebHistory(window.location.pathname),
    history: createWebHashHistory(),
    routes
})

export default router
