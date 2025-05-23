import axios from "axios";
import config from "@/functions/config";
import type { UserType } from "@/types";

export const getUsers = async (): Promise<UserType[]> => {
    const response = await axios.get(config.defaultServer() + "/api/users");
    return response.data;
};
