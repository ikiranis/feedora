import axios from "axios";
import config from "@/functions/config.ts";
import type { UnreadCountsType } from "@/types";

/**
 * Fetch all posts from the backend for the current user.
 * @param page - The page number (1-indexed)
 * @param pageSize - The number of posts per page
 * @param search - Optional search term to filter posts by title
 * @param folderId - Optional folder id to only return posts of that folder's feeds
 * @returns {Promise<any>} List of posts
 * @throws Error if the request fails
 */
export const getPosts = async (page: number, pageSize: number, search?: string, folderId?: string | null) => {
    try {
        const params: any = { page, pageSize };
        if (search && search.trim()) {
            params.search = search.trim();
        }
        if (folderId) {
            params.folderId = folderId;
        }
        
        const response = await axios.get(config.defaultServer() + '/api/posts/getPosts', {
            params,
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
 * Fetch the unread post counts for the current user, in total and per folder.
 * @returns {Promise<UnreadCountsType>} Total unread count and a map of folder id to unread count
 * @throws Error if the request fails
 */
export const getUnreadCounts = async (): Promise<UnreadCountsType> => {
    const response = await axios.get(config.defaultServer() + '/api/posts/unreadCounts');
    return response.data;
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

/**
 * Mark a specific post as read.
 * @param {string} postId - The UUID of the post to mark as read
 * @returns {Promise<any>} Result message from backend
 * @throws Error if the request fails
 */
export const markAsRead = async (postId: string) => {
    try {
        const response = await axios.put(config.defaultServer() + `/api/posts/markAsRead/${postId}`);
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};
