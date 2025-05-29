import { ref, computed } from 'vue';
import { login as apiLogin, register as apiRegister, logout as apiLogout, getCurrentUser, LoginRequest, RegisterRequest, AuthResponse, User } from '@/api/auth';
import axios from 'axios';

// Authentication state
const token = ref<string | null>(localStorage.getItem('auth_token'));
const user = ref<User | null>(null);
const isLoading = ref(false);

// Computed properties
const isAuthenticated = computed(() => !!token.value);

// Set up axios default headers when token changes
const updateAxiosToken = (newToken: string | null) => {
    if (newToken) {
        axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
    } else {
        delete axios.defaults.headers.common['Authorization'];
    }
};

// Initialize axios with existing token
if (token.value) {
    updateAxiosToken(token.value);
}

export const authStore = {
    // State (reactive refs)
    token,
    user,
    isLoading,
    
    // Getters
    isAuthenticated,
    
    // Actions
    async login(loginRequest: LoginRequest): Promise<void> {
        isLoading.value = true;
        try {
            const response: AuthResponse = await apiLogin(loginRequest);
            
            // Store token and update axios headers
            token.value = response.token;
            localStorage.setItem('auth_token', response.token);
            updateAxiosToken(response.token);
            
            // Set user data from response
            user.value = {
                id: 0, // Backend response doesn't include ID in AuthResponse
                username: response.username,
                email: response.email
            };
            
        } catch (error) {
            token.value = null;
            user.value = null;
            localStorage.removeItem('auth_token');
            updateAxiosToken(null);
            throw error;
        } finally {
            isLoading.value = false;
        }
    },
    
    async register(registerRequest: RegisterRequest): Promise<void> {
        isLoading.value = true;
        try {
            const response: AuthResponse = await apiRegister(registerRequest);
            
            // Store token and update axios headers
            token.value = response.token;
            localStorage.setItem('auth_token', response.token);
            updateAxiosToken(response.token);
            
            // Set user data from response
            user.value = {
                id: 0, // Backend response doesn't include ID in AuthResponse
                username: response.username,
                email: response.email
            };
            
        } catch (error) {
            token.value = null;
            user.value = null;
            localStorage.removeItem('auth_token');
            updateAxiosToken(null);
            throw error;
        } finally {
            isLoading.value = false;
        }
    },
    
    async fetchCurrentUser(): Promise<void> {
        if (!token.value) return;
        
        try {
            user.value = await getCurrentUser();
        } catch (error) {
            // If fetching user fails, token might be invalid
            this.logout();
            throw error;
        }
    },
    
    async logout(): Promise<void> {
        try {
            if (token.value) {
                await apiLogout();
            }
        } catch (error) {
            // Ignore logout API errors and proceed with local cleanup
            console.warn('Logout API call failed:', error);
        }
        
        // Clear local state regardless of API call result
        token.value = null;
        user.value = null;
        localStorage.removeItem('auth_token');
        updateAxiosToken(null);
    },
    
    // Initialize authentication state (call on app startup)
    async initialize(): Promise<void> {
        const storedToken = localStorage.getItem('auth_token');
        if (storedToken) {
            token.value = storedToken;
            updateAxiosToken(storedToken);
            
            try {
                await this.fetchCurrentUser();
            } catch (error) {
                // Token is invalid, clear it
                this.logout();
            }
        }
    }
};
