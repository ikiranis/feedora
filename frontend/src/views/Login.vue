<template>
    <div class="login-container d-flex align-items-center justify-content-center min-vh-100">
        <div class="card shadow-lg" style="width: 100%; max-width: 400px;">
            <div class="card-body p-4">
                <div class="text-center mb-4">
                    <h2 class="card-title">{{ language.get('Login') }}</h2>
                    <p class="text-muted">{{ language.get('Please sign in to continue') }}</p>
                </div>
                
                <form @submit.prevent="handleLogin">
                    <div class="mb-3">
                        <label for="email" class="form-label">{{ language.get('Email') }}</label>
                        <input 
                            type="email" 
                            class="form-control" 
                            id="email"
                            v-model="email"
                            :class="{ 'is-invalid': errors.email }"
                            required 
                        />
                        <div v-if="errors.email" class="invalid-feedback">
                            {{ errors.email }}
                        </div>
                    </div>
                    
                    <div class="mb-3">
                        <label for="password" class="form-label">{{ language.get('Password') }}</label>
                        <input 
                            type="password" 
                            class="form-control" 
                            id="password"
                            v-model="password"
                            :class="{ 'is-invalid': errors.password }"
                            required 
                        />
                        <div v-if="errors.password" class="invalid-feedback">
                            {{ errors.password }}
                        </div>
                    </div>
                    
                    <div v-if="errors.general" class="alert alert-danger" role="alert">
                        {{ errors.general }}
                    </div>
                    
                    <button 
                        type="submit" 
                        class="btn btn-primary w-100 mb-3"
                        :disabled="authStore.isLoading.value"
                    >
                        <span v-if="authStore.isLoading.value" class="spinner-border spinner-border-sm me-2" role="status"></span>
                        {{ authStore.isLoading.value ? language.get('Signing In...') : language.get('Sign In') }}
                    </button>
                </form>
                
                <div class="text-center">
                    <p class="mb-0">
                        {{ language.get('Don\'t have an account?') }}
                        <a href="#" @click.prevent="$emit('showRegister')" class="text-decoration-none">
                            {{ language.get('Sign Up') }}
                        </a>
                    </p>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { language } from '@/functions/languageStore';
import { authStore } from '@/functions/authStore';

// Router
const router = useRouter();

// Emits
defineEmits<{
    showRegister: []
}>();

// Form data
const email = ref('');
const password = ref('');

// Error handling
const errors = ref<{
    email?: string;
    password?: string;
    general?: string;
}>({});

// Clear errors when user types
const clearErrors = () => {
    errors.value = {};
};

// Handle login submission
const handleLogin = async () => {
    clearErrors();
    
    // Basic validation
    if (!email.value) {
        errors.value.email = language.get('Email is required');
        return;
    }
    
    if (!password.value) {
        errors.value.password = language.get('Password is required');
        return;
    }
    
    try {
        await authStore.login({
            email: email.value,
            password: password.value
        });
        
        // Login successful - redirect to posts
        await router.push('/posts');
    } catch (error: any) {
        console.error('Login error:', error);
        
        if (error.response?.status === 401) {
            errors.value.general = language.get('Invalid email or password');
        } else if (error.response?.status === 400) {
            errors.value.general = language.get('Please check your input');
        } else {
            errors.value.general = language.get('Login failed. Please try again.');
        }
    }
};

// Watch form inputs to clear errors
import { watch } from 'vue';
watch([email, password], clearErrors);
</script>

<style scoped lang="scss">
.login-container {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    min-height: 100vh;
}

.card {
    border: none;
    border-radius: 15px;
}

.btn-primary {
    border-radius: 8px;
    padding: 12px;
    font-weight: 500;
}

.form-control {
    border-radius: 8px;
    padding: 12px;
    border: 1px solid #ddd;
    
    &:focus {
        border-color: #667eea;
        box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
    }
}

[data-bs-theme="dark"] {
    .login-container {
        background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
    }
    
    .card {
        background-color: var(--bs-dark);
        color: var(--bs-light);
    }
    
    .form-control {
        background-color: var(--bs-dark);
        border-color: #495057;
        color: var(--bs-light);
        
        &:focus {
            background-color: var(--bs-dark);
            border-color: #667eea;
            color: var(--bs-light);
        }
    }
}
</style>
