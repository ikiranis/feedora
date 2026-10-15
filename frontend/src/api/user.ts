import axios from "axios";
import config from "@/functions/config";
import type { UserFormType, UserWithStatisticsType } from "@/types";

export const getUsers = async (): Promise<UserWithStatisticsType[]> => {
    const response = await axios.get(config.defaultServer() + "/api/users");
    return response.data;
};

export const addUser = async (user: UserFormType): Promise<string> => {
    const response = await axios.post(config.defaultServer() + "/api/users", user);
    return response.data;
};

export const updateUser = async (userId: string, user: UserFormType): Promise<string> => {
    const response = await axios.put(config.defaultServer() + "/api/users/" + userId, user);
    return response.data;
};

export const deleteUser = async (userId: string): Promise<string> => {
    const response = await axios.delete(config.defaultServer() + "/api/users/" + userId);
    return response.data;
};
