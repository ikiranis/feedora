<template>
    <div class="register-container d-flex align-items-center justify-content-center min-vh-100">
        <div class="card shadow-lg" style="width: 100%; max-width: 400px;">
            <div class="card-body p-4">
                <div class="text-center mb-4">
                    <h2 class="card-title">{{ language.get('Create Account') }}</h2>
                    <p class="text-muted">{{ language.get('Join us today') }}</p>
                </div>
                
                <form @submit.prevent="handleRegister">
                    <div class="mb-3">
                        <label for="username" class="form-label">{{ language.get('Username') }}</label>
                        <input 
                            type="text" 
                            class="form-control" 
                            id="username"
                            v-model="username"
                            :class="{ 'is-invalid': errors.username }"
                            required 
                        />
                        <div v-if="errors.username" class="invalid-feedback">
                            {{ errors.username }}
                        </div>
                    </div>
                    
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
                        <div class="form-text">{{ language.get('Password must be at least 6 characters long') }}</div>
                    </div>
                    
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">{{ language.get('Confirm Password') }}</label>
                        <input 
                            type="password" 
                            class="form-control" 
                            id="confirmPassword"
                            v-model="confirmPassword"
                            :class="{ 'is-invalid': errors.confirmPassword }"
                            required 
                        />
                        <div v-if="errors.confirmPassword" class="invalid-feedback">
                            {{ errors.confirmPassword }}
                        </div>
                    </div>
                    
                    <div v-if="errors.general" class="alert alert-danger" role="alert">
                        {{ errors.general }}
                    </div>
                    
                    <button 
                        type="submit" 
                        class="btn btn-success w-100 mb-3"
                        :disabled="authStore.isLoading.value"
                    >
                        <span v-if="authStore.isLoading.value" class="spinner-border spinner-border-sm me-2" role="status"></span>
                        {{ authStore.isLoading.value ? language.get('Creating Account...') : language.get('Create Account') }}
                    </button>
                </form>
                
                <div class="text-center">
                    <p class="mb-0">
                        {{ language.get('Already have an account?') }}
                        <a href="#" @click.prevent="$emit('showLogin')" class="text-decoration-none">
                            {{ language.get('Sign in') }}
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
    showLogin: []
}>();

// Form data
const username = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');

// Error handling
const errors = ref<{
    username?: string;
    email?: string;
    password?: string;
    confirmPassword?: string;
    general?: string;
}>({});

// Clear errors when user types
const clearErrors = () => {
    errors.value = {};
};

// Handle register submission
const handleRegister = async () => {
    clearErrors();
    
    // Validation
    if (!username.value) {
        errors.value.username = language.get('Username is required');
        return;
    }
    
    if (username.value.length < 3) {
        errors.value.username = language.get('Username must be at least 3 characters long');
        return;
    }
    
    if (!email.value) {
        errors.value.email = language.get('Email is required');
        return;
    }
    
    if (!password.value) {
        errors.value.password = language.get('Password is required');
        return;
    }
    
    if (password.value.length < 6) {
        errors.value.password = language.get('Password must be at least 6 characters long');
        return;
    }
    
    if (!confirmPassword.value) {
        errors.value.confirmPassword = language.get('Please confirm your password');
        return;
    }
    
    if (password.value !== confirmPassword.value) {
        errors.value.confirmPassword = language.get('Passwords do not match');
        return;
    }
    
    try {
        await authStore.register({
            username: username.value,
            email: email.value,
            password: password.value
        });
        
        // Registration successful - redirect to posts
        await router.push('/posts');
    } catch (error: any) {
        console.error('Registration error:', error);
        
        if (error.response?.status === 400) {
            const errorMessage = error.response?.data?.message || error.response?.data?.error;
            if (errorMessage?.includes('email')) {
                errors.value.email = language.get('Email is already in use');
            } else if (errorMessage?.includes('username')) {
                errors.value.username = language.get('Username is already taken');
            } else {
                errors.value.general = language.get('Please check your input');
            }
        } else {
            errors.value.general = language.get('Registration failed. Please try again.');
        }
    }
};

// Watch form inputs to clear errors
import { watch } from 'vue';
watch([username, email, password, confirmPassword], clearErrors);
</script>

<style scoped lang="scss">
.register-container {
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    min-height: 100vh;
}

.card {
    border: none;
    border-radius: 15px;
}

.btn-success {
    border-radius: 8px;
    padding: 12px;
    font-weight: 500;
}

.form-control {
    border-radius: 8px;
    padding: 12px;
    border: 1px solid #ddd;
    
    &:focus {
        border-color: #11998e;
        box-shadow: 0 0 0 0.2rem rgba(17, 153, 142, 0.25);
    }
}

[data-bs-theme="dark"] {
    .register-container {
        background: linear-gradient(135deg, #0f3460 0%, #0e4b99 100%);
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
            border-color: #11998e;
            color: var(--bs-light);
        }
    }
}
</style>
