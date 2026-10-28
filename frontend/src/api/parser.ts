import axios from "axios";
import config from "@/functions/config";
import type { ParseAnalysisType, ParsePageType } from "@/types";

export interface ParsePageRequest {
    url?: string;
    title: string;
    selector: string;
    frequency: number;
}

/**
 * Get the pages the current user parses
 */
export const getParsePages = async (): Promise<ParsePageType[]> => {
    const response = await axios.get(config.defaultServer() + '/api/parser/pages');
    return response.data;
};

/**
 * Download a page and find its repeated elements, to choose the one that holds the posts.
 * Fails with { error, blocked } when the page blocks the access or has no posts.
 */
export const analyzePage = async (url: string): Promise<ParseAnalysisType> => {
    const response = await axios.post(config.defaultServer() + '/api/parser/analyze', { url });
    return response.data;
};

/**
 * Add a page to parse; it's parsed right away
 */
export const addParsePage = async (page: ParsePageRequest): Promise<ParsePageType> => {
    const response = await axios.post(config.defaultServer() + '/api/parser/pages', page);
    return response.data;
};

/**
 * Change the title, element and frequency of a page; it's parsed again right away
 */
export const updateParsePage = async (id: string, page: ParsePageRequest): Promise<ParsePageType> => {
    const response = await axios.put(config.defaultServer() + '/api/parser/pages/' + id, page);
    return response.data;
};

/**
 * Stop parsing a page and delete its feed file
 */
export const deleteParsePage = async (id: string): Promise<string> => {
    const response = await axios.delete(config.defaultServer() + '/api/parser/pages/' + id);
    return response.data;
};

/**
 * Parse a page now
 */
export const parsePageNow = async (id: string): Promise<ParsePageType> => {
    const response = await axios.post(config.defaultServer() + '/api/parser/pages/' + id + '/parse');
    return response.data;
};

/**
 * Add the generated feed of a page to the user's feeds
 * @param folderId - The folder of the feed, null for the default folder
 */
export const addParsePageToFeeds = async (id: string, folderId: string | null): Promise<string> => {
    const response = await axios.post(config.defaultServer() + '/api/parser/pages/' + id + '/addFeed', { folderId });
    return response.data;
};
