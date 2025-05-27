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
 * Import feeds from uploaded OPML file
 * @param file - The OPML file to upload
 */
export const importOPML = async (file: File) => {
    try {
        const formData = new FormData();
        formData.append('file', file);
        
        const response = await axios.post(config.defaultServer() + '/api/importOPML', formData, {
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
