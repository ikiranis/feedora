import axios from "axios";
import config from "@/functions/config.ts";

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
}

export interface AuthResponse {
    token: string;
    email: string;
    username: string;
}

export interface User {
    id: number;
    username: string;
    email: string;
}

/**
 * Login user with email and password
 */
export const login = async (loginRequest: LoginRequest): Promise<AuthResponse> => {
    const response = await axios.post(config.defaultServer() + '/api/auth/login', loginRequest);
    return response.data;
};

/**
 * Register new user
 */
export const register = async (registerRequest: RegisterRequest): Promise<AuthResponse> => {
    const response = await axios.post(config.defaultServer() + '/api/auth/register', registerRequest);
    return response.data;
};

/**
 * Get current user information
 */
export const getCurrentUser = async (): Promise<User> => {
    const response = await axios.get(config.defaultServer() + '/api/auth/me');
    return response.data;
};

/**
 * Logout user
 */
export const logout = async (): Promise<void> => {
    await axios.post(config.defaultServer() + '/api/auth/logout');
};
