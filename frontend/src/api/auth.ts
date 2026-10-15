import axios from "axios";
import config from "@/functions/config.ts";
import type { Role } from "@/types";

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
    userId: string;
    role: Role;
}

export interface User {
    id: string;
    username: string;
    email: string;
    role: Role;
}

export interface AuthStatus {
    // False until the first user, the admin, registers
    hasUsers: boolean;
    registrationEnabled: boolean;
}

/**
 * Get whether there are users yet and whether new users can register
 */
export const getAuthStatus = async (): Promise<AuthStatus> => {
    const response = await axios.get(config.defaultServer() + '/api/auth/status');
    return response.data;
};

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
    return toUser(response.data);
};

/**
 * Converts an auth response to the user it describes
 */
export const toUser = (response: AuthResponse): User => ({
    id: response.userId,
    username: response.username,
    email: response.email,
    role: response.role
});

/**
 * Logout user
 */
export const logout = async (): Promise<void> => {
    await axios.post(config.defaultServer() + '/api/auth/logout');
};
