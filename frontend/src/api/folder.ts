import axios from "axios";
import config from "@/functions/config";
import type { FolderType } from "@/types";

export const getFolders = async (): Promise<FolderType[]> => {
    const response = await axios.get(config.defaultServer() + "/api/folders");
    return response.data;
};

export const addFolder = async (folderData: { name: string }): Promise<string> => {
    const response = await axios.post(config.defaultServer() + "/api/addFolder", folderData);
    return response.data;
};

export const deleteFolder = async (folderId: string): Promise<string> => {
    const response = await axios.delete(config.defaultServer() + "/api/deleteFolder/" + folderId);
    return response.data;
};
