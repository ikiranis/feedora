<template>
    <div class="auth-wrapper">
        <Login v-if="showLogin" @showRegister="showLogin = false" />
        <Register v-else @showLogin="showLogin = true" />
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import Login from './Login.vue';
import Register from './Register.vue';

const showLogin = ref(true);
</script>

<style scoped lang="scss">
.auth-wrapper {
    width: 100%;
    height: 100vh;
}
</style>
