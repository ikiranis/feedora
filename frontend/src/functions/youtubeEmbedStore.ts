import { reactive } from "vue";

// Embeddability by video id: true/false once checked. Reactive so the player replaces the thumbnail when the check passes.
const results = reactive(new Map<string, boolean>());
const pending = new Set<string>();

/**
 * Checks through YouTube's oEmbed endpoint whether a video can be played embedded.
 * It answers 401/403/404 for videos whose uploader disabled embedding, or that are private or removed.
 * Network errors, rate limiting and server errors count as embeddable, so the player is still tried.
 */
const check = (videoId: string) => {
    if (results.has(videoId) || pending.has(videoId)) return;

    pending.add(videoId);
    const videoUrl = `https://www.youtube.com/watch?v=${videoId}`;
    fetch(`https://www.youtube.com/oembed?format=json&url=${encodeURIComponent(videoUrl)}`)
        .then(response => { results.set(videoId, response.ok || response.status === 429 || response.status >= 500); })
        .catch(() => { results.set(videoId, true); })
        .finally(() => { pending.delete(videoId); });
};

export const youtubeEmbedStore = {
    check,
    // True when the video can be embedded, false when it can't, undefined while it's being checked
    isEmbeddable: (videoId: string) => results.get(videoId),
};
