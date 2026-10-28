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
// A feed found on a web page, when adding a feed by the site's URL
export interface DiscoveredFeed {
    url: string;
    title: string | null;
}

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
    // Null for pages saved from the browser (see the extension)
    feed: Feed | null;
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

// A post read from a parsed page
export interface ParsePageItemType {
    title: string;
    link: string;
    description: string | null;
    image: string | null;
    published: string | null;
}

// Repeated elements of a page that may hold its posts
export interface ParseCandidateType {
    selector: string;
    count: number;
    samples: ParsePageItemType[];
}

// A page analyzed by the parser, with the elements to choose the posts from
export interface ParseAnalysisType {
    url: string;
    title: string;
    candidates: ParseCandidateType[];
}

// A page without a feed, parsed into an RSS feed
export interface ParsePageType {
    id: string;
    url: string;
    title: string;
    selector: string;
    // Minutes between two parses
    frequency: number;
    feedName: string;
    // Path of the generated feed on the server, e.g. /api/parser/feed/example.com.xml
    feedPath: string;
    createdAt: string;
    lastParsedAt: string | null;
    lastItemCount: number;
    // Error of the last parse, null when it succeeded
    lastError: string | null;
    // The generated feed is already in the user's feeds
    inFeeds: boolean;
}
