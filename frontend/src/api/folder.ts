import axios from "axios";
import config from "@/functions/config";
import type { FolderType } from "@/types";

export const getFolders = async (): Promise<FolderType[]> => {
    const response = await axios.get(config.defaultServer() + "/api/folders");
    return response.data;
};

export const addFolder = async (name: string): Promise<string> => {
    const response = await axios.post(config.defaultServer() + "/api/addFolder", name, {
        headers: {
            'Content-Type': 'text/plain'
        }
    });
    return response.data;
};

export const deleteFolder = async (folderId: string): Promise<string> => {
    const response = await axios.delete(config.defaultServer() + "/api/deleteFolder/" + folderId);
    return response.data;
};

export const updateFolder = async (folderId: string, name: string): Promise<string> => {
    const response = await axios.put(config.defaultServer() + "/api/updateFolder/" + folderId, name, {
        headers: {
            'Content-Type': 'text/plain'
        }
    });
    return response.data;
};
