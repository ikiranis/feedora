import axios from "axios";
import config from "@/functions/config.ts";
import type { PostType, UnreadCountsType } from "@/types";

/**
 * Fetch all posts from the backend for the current user.
 * @param page - The page number (1-indexed)
 * @param pageSize - The number of posts per page
 * @param search - Optional search term to filter posts by title
 * @param folderId - Optional folder id to only return posts of that folder's feeds
 * @param feedId - Optional feed id to only return posts of that feed
 * @param saved - If true, return the saved posts (read or unread) instead of the unread ones
 * @param favorite - If true, return the favorite posts (read or unread) instead. Takes precedence over saved.
 * @param includeRead - If true, return the read posts too, not only the unread ones
 * @param hideShortsFolderIds - Folders whose YouTube Shorts are left out (not applied to the saved and favorite posts)
 * @returns {Promise<{posts: any[], listedAt: string | null}>} List of posts, and the server time of the listing,
 *          for markAllAsRead to skip the posts added after it
 * @throws Error if the request fails
 */
export const getPosts = async (page: number, pageSize: number, search?: string, folderId?: string | null, feedId?: string | null, saved?: boolean, includeRead?: boolean,
                               hideShortsFolderIds?: string[], favorite?: boolean): Promise<{ posts: any[], listedAt: string | null }> => {
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
        if (favorite) {
            params.favorite = true;
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
        return { posts: response.data, listedAt: response.headers['x-listed-at'] ?? null };
    } catch (error: any) {
        throw error;
    }
};

/**
 * Fetch the unread post counts for the current user, in total, per folder and per feed.
 * @param hideShortsFolderIds - Folders whose YouTube Shorts aren't counted
 * @returns {Promise<UnreadCountsType>} Total unread count, saved and favorite posts counts, a map of folder id to unread count and the feeds with unread posts
 * @throws Error if the request fails
 */
export const getUnreadCounts = async (hideShortsFolderIds?: string[]): Promise<UnreadCountsType> => {
    const params = hideShortsFolderIds?.length ? { hideShortsFolderIds: hideShortsFolderIds.join(',') } : {};
    const response = await axios.get(config.defaultServer() + '/api/posts/unreadCounts', { params });
    return response.data;
};

/**
 * Count the unread posts of a view (all, a folder or a feed) added after the list was loaded, e.g. by a parse in the meantime.
 * @param addedAfter - Server time of the listing (from getPosts' listedAt)
 * @param folderId - Optional folder id to only count posts of that folder's feeds
 * @param feedId - Optional feed id to only count posts of that feed
 * @param hideShortsFolderIds - Folders whose YouTube Shorts aren't counted
 * @returns {Promise<number>} The number of new unread posts
 * @throws Error if the request fails
 */
export const countNewPosts = async (addedAfter: string, folderId?: string | null, feedId?: string | null,
                                    hideShortsFolderIds?: string[]): Promise<number> => {
    const params: any = { addedAfter };
    if (folderId) {
        params.folderId = folderId;
    }
    if (feedId) {
        params.feedId = feedId;
    }
    if (hideShortsFolderIds?.length) {
        params.hideShortsFolderIds = hideShortsFolderIds.join(',');
    }
    const response = await axios.get(config.defaultServer() + '/api/posts/countNew', { params });
    return response.data.count;
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
 * Fetch the article of the page a post's link preview shows (e.g. the article a tweet links to), to show it inside the post.
 * @param {string} postId - The UUID of the post
 * @returns {Promise<string | null>} Sanitized article HTML, or null if none could be read
 * @throws Error if the request fails
 */
export const getLinkContent = async (postId: string): Promise<string | null> => {
    const response = await axios.get(config.defaultServer() + `/api/posts/linkContent/${postId}`);
    return response.data.content;
};

/**
 * Trigger backend to parse the RSS feeds and update posts for the current user.
 * Only the feeds of a folder, or a single feed, are parsed when given; otherwise all of them.
 * @param {string | null} folderId - The folder whose feeds to parse, or null
 * @param {string | null} feedId - The feed to parse, or null (takes precedence over the folder)
 * @returns {Promise<any>} Result message from backend
 * @throws Error if the request fails
 */
export const parseFeeds = async (folderId: string | null = null, feedId: string | null = null) => {
    try {
        const params: Record<string, string> = {};
        if (feedId) params.feedId = feedId;
        else if (folderId) params.folderId = folderId;
        const response = await axios.post(config.defaultServer() + '/api/posts/parseFeeds', null, { params });
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
 * @param {string | null} addedBefore - Optional server time (from getPosts' listedAt), to only mark posts added up to it
 * @returns {Promise<number>} The number of posts marked as read
 * @throws Error if the request fails
 */
export const markAllAsRead = async (folderId?: string | null, feedId?: string | null, olderThanDays?: number | null,
                                    addedBefore?: string | null): Promise<number> => {
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
    if (addedBefore) {
        params.addedBefore = addedBefore;
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

/**
 * Add a post to the favorites, or remove it from them. Favorite posts are never deleted by the cleanups.
 * @param {string} postId - The UUID of the post
 * @param {boolean} favorite - true to add the post to the favorites, false to remove it from them
 * @throws Error if the request fails
 */
export const setPostFavorite = async (postId: string, favorite: boolean) => {
    await axios.put(config.defaultServer() + `/api/posts/favorite/${postId}`, null, { params: { favorite } });
};

/**
 * Fetch the posts (read or unread) added after a time, for the live newsfeed.
 * @param addedAfter - Server time to check from (from getPosts' or a previous getLivePosts' listedAt)
 * @returns {Promise<{posts: PostType[], listedAt: string | null}>} The posts, most recently added first,
 *          and the server time to check from next
 * @throws Error if the request fails
 */
export const getLivePosts = async (addedAfter: string): Promise<{ posts: PostType[], listedAt: string | null }> => {
    const response = await axios.get(config.defaultServer() + '/api/posts/live', { params: { addedAfter } });
    return { posts: response.data, listedAt: response.headers['x-listed-at'] ?? null };
};

/**
 * How often the live newsfeed checks for new posts, from the settings.
 * @returns {Promise<number>} The interval in seconds
 * @throws Error if the request fails
 */
export const getLiveRefreshSeconds = async (): Promise<number> => {
    const response = await axios.get(config.defaultServer() + '/api/posts/live/refreshSeconds');
    return response.data.seconds;
};
