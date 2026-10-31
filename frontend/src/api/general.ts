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

export interface Release {
    version: string
    url: string
}

/**
 * Get the latest published release of Feedora from GitHub (drafts and pre-releases are excluded).
 * Uses fetch instead of axios, so the authorization header of the app isn't sent to GitHub.
 */
export const getLatestRelease = async (): Promise<Release | null> => {
    try {
        const response = await fetch('https://api.github.com/repos/ikiranis/feedora/releases/latest', {
            headers: { 'Accept': 'application/vnd.github+json' }
        })

        if (!response.ok) {
            return null
        }

        const data = await response.json()
        return { version: String(data.tag_name).replace(/^v/i, ''), url: data.html_url }
    } catch(error: any) {
        return null
    }
}

/**
 * Check if version a is newer than version b, comparing the numeric parts, e.g. 1.10.0 > 1.9.2
 */
export const isNewerVersion = (a: string, b: string): boolean => {
    const partsA = a.split(/[.-]/).map(part => parseInt(part, 10) || 0)
    const partsB = b.split(/[.-]/).map(part => parseInt(part, 10) || 0)

    for (let i = 0; i < Math.max(partsA.length, partsB.length); i++) {
        const diff = (partsA[i] ?? 0) - (partsB[i] ?? 0)
        if (diff !== 0) {
            return diff > 0
        }
    }

    return false
}
