import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Get all feeds from the backend
 */
export const getFeeds = async () => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/getAllFeeds', {
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
 * Import feeds from OPML file (feeds.xml on backend)
 */
export const importOPML = async () => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/importOPML');
        if (response.status === 200) {
            return response.data;
        }
    } catch (error: any) {
        throw error;
    }
};
