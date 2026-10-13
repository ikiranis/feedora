import { reactive } from "vue";
import { getPostContent } from "@/api/post";
import type { PostType } from "@/types";

const MAX_CONCURRENT_PREFETCHES = 2;

// Loaded full contents by post id (null when the post has no content). Reactive so posts render as soon as it arrives.
const contents = reactive(new Map<string, string | null>());
const pending = new Map<string, Promise<void>>();
const loadingIds = reactive(new Set<string>()); // Reactive mirror of the pending ids
const prefetchQueue: string[] = [];
let activePrefetches = 0;

/**
 * True for YouTube links, which show the video instead of the article content.
 */
const isYoutubeLink = (link: string): boolean => {
    try {
        return /(^|\.)(youtube\.com|youtube-nocookie\.com|youtu\.be)$/.test(new URL(link).hostname);
    } catch {
        return false;
    }
};

/**
 * Loads the full content of a post into the cache, unless it's already loaded or loading.
 * Failed loads aren't cached, so they're retried the next time.
 */
const load = (postId: string): Promise<void> => {
    if (contents.has(postId)) return Promise.resolve();

    let request = pending.get(postId);
    if (!request) {
        request = getPostContent(postId)
            .then(content => { contents.set(postId, content); })
            .catch(error => { console.error('Failed to load post content:', error); })
            .finally(() => {
                pending.delete(postId);
                loadingIds.delete(postId);
            });
        pending.set(postId, request);
        loadingIds.add(postId);
    }
    return request;
};

/**
 * Starts queued prefetches while there are free slots.
 */
const runPrefetchQueue = () => {
    while (activePrefetches < MAX_CONCURRENT_PREFETCHES && prefetchQueue.length > 0) {
        const postId = prefetchQueue.shift()!;
        if (contents.has(postId) || pending.has(postId)) continue;

        activePrefetches++;
        load(postId).finally(() => {
            activePrefetches--;
            runPrefetchQueue();
        });
    }
};

/**
 * Loads the content of the given posts in the background, a few at a time, so they open without waiting.
 * Replaces previously queued prefetches, since only the posts near the current one are worth loading.
 */
const prefetch = (posts: PostType[]) => {
    prefetchQueue.length = 0;
    for (const post of posts) {
        if (!isYoutubeLink(post.link) && !contents.has(post.id) && !pending.has(post.id)) {
            prefetchQueue.push(post.id);
        }
    }
    runPrefetchQueue();
};

export const postContentStore = {
    get: (postId: string) => contents.get(postId),
    has: (postId: string) => contents.has(postId),
    isLoading: (postId: string) => loadingIds.has(postId),
    load,
    prefetch,
    isYoutubeLink,
};
