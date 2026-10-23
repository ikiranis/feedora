import { reactive } from "vue";
import { getLinkContent } from "@/api/post";

// Loaded articles of the posts' links by post id (null when none could be read). Reactive so posts render as soon as it arrives.
const contents = reactive(new Map<string, string | null>());
const loadingIds = reactive(new Set<string>());
// Posts whose link's article is shown inside them
const shownIds = reactive(new Set<string>());

/**
 * Loads the article of a post's link into the cache, unless it's already loaded or loading.
 * Failed requests aren't cached, so they're retried the next time it's shown.
 */
const load = (postId: string) => {
    if (contents.has(postId) || loadingIds.has(postId)) return;

    loadingIds.add(postId);
    getLinkContent(postId)
        .then(content => { contents.set(postId, content); })
        .catch(error => { console.error('Failed to load the link content:', error); })
        .finally(() => { loadingIds.delete(postId); });
};

/**
 * Shows the article of a post's link inside the post, loading it if needed, or hides it.
 */
const toggle = (postId: string) => {
    if (shownIds.has(postId)) {
        shownIds.delete(postId);
    } else {
        shownIds.add(postId);
        load(postId);
    }
};

export const linkContentStore = {
    get: (postId: string) => contents.get(postId),
    has: (postId: string) => contents.has(postId),
    isLoading: (postId: string) => loadingIds.has(postId),
    isShown: (postId: string) => shownIds.has(postId),
    toggle,
};
