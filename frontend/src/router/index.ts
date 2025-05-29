// You have to add at include declaration the "router/**/*.ts", on tsconfig.json
// and install vue-router with npm

import {RouteRecordRaw, createRouter, createWebHashHistory} from "vue-router"
import Home from "@/views/Home.vue"
import Settings from "@/views/setting/Settings.vue"
import Setting from "@/views/setting/Setting.vue"
import Feed from "@/views/Feed.vue"
import Folder from "@/views/Folder.vue"
import User from "@/views/User.vue"
import Post from "@/views/Post.vue"
import Auth from "@/views/Auth.vue"
import { authStore } from "@/functions/authStore"

const routes : RouteRecordRaw[] = [
    {
        path: '/auth',
        name: 'Auth',
        component: Auth,
        meta: { requiresGuest: true }
    },
    {
        path: '/',
        redirect: '/posts',
        meta: { requiresAuth: true }
    },
    {
        path: '/home',
        name: 'Home',
        component: Home,
        meta: { requiresAuth: true }
    },
    {
        path: '/feeds',
        name: 'Feeds',
        component: Feed,
        meta: { requiresAuth: true }
    },
    {
        path: '/settings',
        name: 'Settings',
        component: Settings,
        meta: { requiresAuth: true }
    },
    {
        path: '/setting/:id',
        name: 'Setting',
        component: Setting,
        props: true,
        meta: { requiresAuth: true }
    },
    {
        path: '/folders',
        name: 'Folders',
        component: Folder,
        meta: { requiresAuth: true }
    },
    {
        path: '/users',
        name: 'Users',
        component: User,
        meta: { requiresAuth: true }
    },
    {
        path: '/posts',
        name: 'Posts',
        component: Post,
        meta: { requiresAuth: true }
    },
]

// Set route path with webHistory, on current path
const router = createRouter({
    // history: createWebHistory(window.location.pathname),
    history: createWebHashHistory(),
    routes
})

// Authentication guards
router.beforeEach(async (to, from, next) => {
    // Wait a tick to ensure reactive state is updated
    await new Promise(resolve => setTimeout(resolve, 0));
    
    const isAuthenticated = authStore.isAuthenticated.value;
    
    // Routes that require authentication
    if (to.meta.requiresAuth && !isAuthenticated) {
        next({ name: 'Auth' });
        return;
    }
    
    // Routes that require guest (not authenticated)
    if (to.meta.requiresGuest && isAuthenticated) {
        next({ path: '/posts' });
        return;
    }
    
    next();
});

export default router
