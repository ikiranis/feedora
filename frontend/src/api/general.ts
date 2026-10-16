import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Check if the app is alive
 */
export const checkAppAlive = async (): Promise<boolean>  => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/general/appAlive')

        if (response.status === 200) {
            return true
        }

        return false
    } catch(error: any) {
        return false
    }
}

/**
 * Get the version of the running app
 */
export const getAppVersion = async (): Promise<string | null> => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/general/version')
        return response.data
    } catch(error: any) {
        return null
    }
}
