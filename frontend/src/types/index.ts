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
    // Color of the feed's cards chosen by the user (#rrggbb), null to use faviconColor
    cardColor?: string | null;
    // Main color of the favicon (#rrggbb): null until checked, empty when it has no clear color
    faviconColor?: string | null;
    // Number of log entries of the feed itself, not counting those of its posts
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
    favorites: number;
    folders: Record<string, number>;
    feeds: FeedUnreadCountType[];
}

export type Role = 'ADMIN' | 'USER';

export interface UserType {
    id: string;
    username: string;
    email: string;
    role: Role;
}

export interface UserWithStatisticsType extends UserType {
    folderCount: number;
    feedCount: number;
    postCount: number;
    unreadCount: number;
    savedCount: number;
    favoriteCount: number;
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
    // Image for the card, found in the feed entry when parsed; null if none (or for posts added before it was kept)
    thumbnail: string | null;
    // Tweets: preview of the first link in the text (page URL, title and image), found when parsed; null if none
    linkUrl: string | null;
    linkTitle: string | null;
    linkImage: string | null;
    // Tweets: MP4 of the tweet's video or GIF, shown instead of its thumbnail; empty or null if none (or not found yet)
    video: string | null;
    pubDate: string | null;
    author: string | null;
    read: boolean;
    saved: boolean;
    favorite: boolean;
    // YouTube videos: scheduled live stream/premiere or live now, and its (scheduled) start
    liveStatus: 'UPCOMING' | 'LIVE' | null;
    liveStartAt: string | null;
    // YouTube videos: length in seconds, null if unknown (e.g. scheduled or live streams)
    duration: number | null;
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
