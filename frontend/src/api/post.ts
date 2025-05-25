import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Fetch all posts from the backend for the current user.
 * @returns {Promise<any>} List of posts
 * @throws Error if the request fails
 */
export const getPosts = async (page: number, pageSize: number) => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/posts/getPosts', {
            params: { page, pageSize },
            headers: { 'Accept': 'application/json' }
        });
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};

/**
 * Trigger backend to parse all RSS feeds and update posts for the current user.
 * @returns {Promise<any>} Result message from backend
 * @throws Error if the request fails
 */
export const parseFeeds = async () => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/posts/parseFeeds');
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};

/**
 * Delete all posts from the backend.
 * @returns {Promise<any>} Result message from backend
 * @throws Error if the request fails
 */
export const deleteAllPosts = async () => {
    try {
        const response = await axios.delete(config.defaultServer() + '/api/posts/deleteAll');
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};
