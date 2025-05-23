import axios from "axios";
import config from "@/functions/config";
import type { FolderType } from "@/types";

export const getFolders = async (): Promise<FolderType[]> => {
    const response = await axios.get(config.defaultServer() + "/api/folders");
    return response.data;
};
