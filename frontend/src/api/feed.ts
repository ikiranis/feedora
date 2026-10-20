import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Get all feeds from the backend
 */
export const getFeeds = async () => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/feed/getAllFeeds', {
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
 * Get paginated feeds from the backend
 * @param page - The page number (1-indexed)
 * @param pageSize - The number of feeds per page
 * @param search - Optional search term to filter feeds by title
 */
export const getFeedsPaginated = async (page: number, pageSize: number, search?: string) => {
    try {
        const params: any = { page, pageSize };
        if (search && search.trim()) {
            params.search = search.trim();
        }
        
        const response = await axios.get(config.defaultServer() + '/api/feed/getFeeds', {
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
 * Import feeds from uploaded OPML file
 * @param file - The OPML file to upload
 */
export const importOPML = async (file: File) => {
    try {
        const formData = new FormData();
        formData.append('file', file);
        
        const response = await axios.post(config.defaultServer() + '/api/feed/importOPML', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};

/**
 * Export the current user's feeds and folders as an OPML file
 * @returns The OPML file as a Blob
 */
export const exportOPML = async (): Promise<Blob> => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/feed/exportOPML', {
            responseType: 'blob'
        });
        return response.data;
    } catch (error: any) {
        throw error;
    }
};

/**
 * Check if a feed operation (parsing or OPML import) is currently running
 */
export const getFeedOperationStatus = async () => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/feed/feedOperationStatus', {
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
 * Get when the feeds of the current user were last parsed
 * @returns The ISO date of the last parse, or null if the feeds were never parsed
 */
export const getLastParsedAt = async (): Promise<string | null> => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/feed/lastParsedAt', {
            headers: { 'Accept': 'application/json' }
        });
        if (response.status === 200) {
            return response.data.lastParsedAt;
        }
        return null;
    } catch (error: any) {
        throw error;
    }
};

/**
 * Add a single feed
 * @param feedData - Object containing url, folderId, and optional title
 */
export const addFeed = async (feedData: { url: string; folderId: string | null; title?: string }) => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/feed/addFeed', feedData, {
            headers: { 'Content-Type': 'application/json' }
        });
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};

/**
 * Fetch feed information from a URL to get title and other metadata
 * @param url - The RSS feed URL
 */
export const fetchFeedInfo = async (url: string) => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/feed/fetchFeedInfo', {
            params: { url },
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
 * Delete a feed by ID
 * @param feedId - The ID of the feed to delete
 */
export const deleteFeed = async (feedId: string) => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/feed/deleteFeed', null, {
            params: { feedId },
            headers: { 'Content-Type': 'application/json' }
        });
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};

/**
 * Update a feed's title and folder
 * @param feedId - The ID of the feed to update
 * @param updateData - Object containing title and folderName
 */
export const updateFeed = async (feedId: string, updateData: { title: string; folderName: string; cardColor: string }) => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/feed/updateFeed', {
            feedId,
            title: updateData.title,
            folderName: updateData.folderName,
            cardColor: updateData.cardColor
        }, {
            headers: { 'Content-Type': 'application/json' }
        });
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};
