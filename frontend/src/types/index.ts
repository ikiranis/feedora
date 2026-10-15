export interface Pageable {
    offset: number;
    pageNumber: number;
    pageSize: number;
    paged: boolean;
    sort: Sort;
    unpaged: boolean;
    totalPages: number;
    sortField: SortField
}

export type Sort = {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
}

export type SortField = {
    field: string;
    order: string;
    enable: boolean;
}

export type Language = {
    lang: string,
    name: string
}

export interface Feed {
    id: string;
    title: string;
    xmlUrl: string;
    htmlUrl: string;
    type: string;
    folderName?: string;
    folderId?: string | null;
    // Number of log entries of the feed and its posts
    logCount?: number;
    // Null until the feed is checked for the first time
    statistics?: FeedStatistics | null;
}

// Parsing statistics of a feed, updated whenever the feed is parsed
export interface FeedStatistics {
    lastCheckedAt: string | null;
    lastSuccessAt: string | null;
    lastNewPostAt: string | null;
    lastPostPubDate: string | null;
    nextCheckAt: string | null;
    checkIntervalMinutes: number;
    postsPerDay: number;
    postsPerHour: number;
    totalChecks: number;
    totalErrors: number;
    consecutiveErrors: number;
    lastErrorAt: string | null;
    lastError: string | null;
    lastEntriesCount: number;
    lastNewPostsCount: number;
    totalNewPosts: number;
    lastFetchDurationMs: number;
}

export interface FolderType {
    id: string;
    name: string;
}

export interface FeedUnreadCountType {
    id: string;
    title: string;
    htmlUrl: string | null;
    folderId: string | null;
    count: number;
}

export interface UnreadCountsType {
    total: number;
    saved: number;
    folders: Record<string, number>;
    feeds: FeedUnreadCountType[];
}

export interface UserType {
    id: string;
    username: string;
    email: string;
}

export interface UserWithStatisticsType extends UserType {
    folderCount: number;
    feedCount: number;
    postCount: number;
    unreadCount: number;
    savedCount: number;
    logCount: number;
}

export interface UserFormType {
    username: string;
    email: string;
    // Empty on update keeps the current password
    password: string;
}

export interface PostType {
    id: string;
    title: string;
    link: string;
    description: string | null;
    pubDate: string | null;
    author: string | null;
    read: boolean;
    saved: boolean;
    feed: Feed;
    user: UserType;
}



export type LogLevel = 'INFO' | 'WARNING' | 'ERROR';

export interface LogEntryType {
    id: string;
    createdAt: string;
    level: LogLevel;
    type: string;
    message: string | null;
    details: string | null;
    feedId: string | null;
    feedTitle: string | null;
    postId: string | null;
    postTitle: string | null;
    url: string | null;
}
