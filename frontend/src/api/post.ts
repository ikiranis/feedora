import axios from "axios";
import config from "@/functions/config.ts";
import type { UnreadCountsType } from "@/types";

/**
 * Fetch all posts from the backend for the current user.
 * @param page - The page number (1-indexed)
 * @param pageSize - The number of posts per page
 * @param search - Optional search term to filter posts by title
 * @param folderId - Optional folder id to only return posts of that folder's feeds
 * @param feedId - Optional feed id to only return posts of that feed
 * @param saved - If true, return the saved posts (read or unread) instead of the unread ones
 * @param includeRead - If true, return the read posts too, not only the unread ones
 * @param hideShortsFolderIds - Folders whose YouTube Shorts are left out (not applied to the saved posts)
 * @returns {Promise<any>} List of posts
 * @throws Error if the request fails
 */
export const getPosts = async (page: number, pageSize: number, search?: string, folderId?: string | null, feedId?: string | null, saved?: boolean, includeRead?: boolean, hideShortsFolderIds?: string[]) => {
    try {
        const params: any = { page, pageSize };
        if (search && search.trim()) {
            params.search = search.trim();
        }
        if (folderId) {
            params.folderId = folderId;
        }
        if (feedId) {
            params.feedId = feedId;
        }
        if (saved) {
            params.saved = true;
        }
        if (includeRead) {
            params.includeRead = true;
        }
        if (hideShortsFolderIds?.length) {
            params.hideShortsFolderIds = hideShortsFolderIds.join(',');
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
 * Fetch the unread post counts for the current user, in total, per folder and per feed.
 * @param hideShortsFolderIds - Folders whose YouTube Shorts aren't counted
 * @returns {Promise<UnreadCountsType>} Total unread count, saved posts count, a map of folder id to unread count and the feeds with unread posts
 * @throws Error if the request fails
 */
export const getUnreadCounts = async (hideShortsFolderIds?: string[]): Promise<UnreadCountsType> => {
    const params = hideShortsFolderIds?.length ? { hideShortsFolderIds: hideShortsFolderIds.join(',') } : {};
    const response = await axios.get(config.defaultServer() + '/api/posts/unreadCounts', { params });
    return response.data;
};

/**
 * Fetch the full content of a post. The backend uses the content from the feed or, if missing,
 * extracts the article from the post's page.
 * @param {string} postId - The UUID of the post
 * @returns {Promise<{content: string | null, logCount: number}>} Sanitized content HTML (null if the post has no content),
 *          and the number of log entries of the post, e.g. when its page couldn't be read
 * @throws Error if the request fails
 */
export const getPostContent = async (postId: string): Promise<{ content: string | null, logCount: number }> => {
    const response = await axios.get(config.defaultServer() + `/api/posts/content/${postId}`);
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

/**
 * Mark a specific post as unread.
 * @param {string} postId - The UUID of the post to mark as unread
 * @returns {Promise<any>} Result message from backend
 * @throws Error if the request fails
 */
export const markAsUnread = async (postId: string) => {
    const response = await axios.put(config.defaultServer() + `/api/posts/markAsUnread/${postId}`);
    return response.data;
};

/**
 * Mark all the unread posts as read, optionally only those of a folder or feed, or older than some days.
 * @param {string | null} folderId - Optional folder id to only mark posts of that folder's feeds
 * @param {string | null} feedId - Optional feed id to only mark posts of that feed
 * @param {number | null} olderThanDays - Optional age in days, to only mark posts published before it
 * @returns {Promise<number>} The number of posts marked as read
 * @throws Error if the request fails
 */
export const markAllAsRead = async (folderId?: string | null, feedId?: string | null, olderThanDays?: number | null): Promise<number> => {
    const params: any = {};
    if (folderId) {
        params.folderId = folderId;
    }
    if (feedId) {
        params.feedId = feedId;
    }
    if (olderThanDays) {
        params.olderThanDays = olderThanDays;
    }
    const response = await axios.put(config.defaultServer() + '/api/posts/markAllAsRead', null, { params });
    return response.data.count;
};

/**
 * Save a post for later, or remove it from the saved posts.
 * @param {string} postId - The UUID of the post
 * @param {boolean} saved - true to save the post, false to remove it from the saved posts
 * @throws Error if the request fails
 */
export const setPostSaved = async (postId: string, saved: boolean) => {
    await axios.put(config.defaultServer() + `/api/posts/saved/${postId}`, null, { params: { saved } });
};
