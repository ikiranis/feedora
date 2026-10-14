import axios from "axios";
import config from "@/functions/config.ts";
import type { LogEntryType, LogLevel } from "@/types";

/**
 * Fetch the log entries of the current user, newest first.
 * @param page - The page number (1-indexed)
 * @param pageSize - The number of entries per page
 * @param feedId - Optional feed id to only return the entries of that feed and its posts
 * @param postId - Optional post id to only return the entries of that post
 * @param level - Optional level to only return the entries of that level
 * @returns {Promise<LogEntryType[]>} List of log entries
 * @throws Error if the request fails
 */
export const getLogs = async (page: number, pageSize: number, feedId?: string | null, postId?: string | null, level?: LogLevel | null): Promise<LogEntryType[]> => {
    const params: any = { page, pageSize };
    if (feedId) {
        params.feedId = feedId;
    }
    if (postId) {
        params.postId = postId;
    }
    if (level) {
        params.level = level;
    }
    const response = await axios.get(config.defaultServer() + '/api/logs', { params });
    return response.data;
};

/**
 * Delete the log entries of the current user, or only those of a feed or a post.
 * @param feedId - Optional feed id to only delete the entries of that feed and its posts
 * @param postId - Optional post id to only delete the entries of that post. Takes precedence over feedId.
 * @returns {Promise<number>} The number of entries deleted
 * @throws Error if the request fails
 */
export const deleteLogs = async (feedId?: string | null, postId?: string | null): Promise<number> => {
    const params: any = {};
    if (feedId) {
        params.feedId = feedId;
    }
    if (postId) {
        params.postId = postId;
    }
    const response = await axios.delete(config.defaultServer() + '/api/logs', { params });
    return response.data.count;
};
