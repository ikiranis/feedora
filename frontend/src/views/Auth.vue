<template>
    <div class="auth-wrapper">
        <template v-if="status">
            <Login v-if="showLogin" :registrationEnabled="status.registrationEnabled" @showRegister="showLogin = false" />
            <Register v-else :firstUser="!status.hasUsers" @showLogin="showLogin = true" @registrationDisabled="registrationDisabled" />
        </template>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import Login from './Login.vue';
import Register from './Register.vue';
import { getAuthStatus, AuthStatus } from '@/api/auth';

const showLogin = ref(true);
const status = ref<AuthStatus | null>(null);

onMounted(async () => {
    try {
        status.value = await getAuthStatus();
    } catch (error) {
        console.error('Failed to get the authentication status:', error);
        status.value = { hasUsers: true, registrationEnabled: false };
    }
    // Without users yet, the first visit creates the admin account
    showLogin.value = status.value.hasUsers;
});

/**
 * The admin disabled the registration while the register page was open
 */
const registrationDisabled = () => {
    status.value = { hasUsers: true, registrationEnabled: false };
    showLogin.value = true;
};
</script>

<style scoped lang="scss">
.auth-wrapper {
    width: 100%;
    height: 100vh;
}
</style>
