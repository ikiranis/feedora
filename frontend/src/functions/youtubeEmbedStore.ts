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

// Players being watched for errors, by their window
const players = new Map<Window, { videoId: string; ready: boolean }>();

/**
 * Player messages: an error means the video can't be played embedded after all
 * (e.g. region or label restrictions that oEmbed doesn't report), so the thumbnail link is shown instead.
 */
window.addEventListener('message', (event: MessageEvent) => {
    if (!/^https:\/\/www\.youtube(-nocookie)?\.com$/.test(event.origin)) return;
    const player = players.get(event.source as Window);
    if (!player || typeof event.data !== 'string') return;

    let data: { event?: string };
    try { data = JSON.parse(event.data); } catch { return; }

    if (!player.ready) {
        player.ready = true;
        (event.source as Window).postMessage(JSON.stringify({ event: 'command', func: 'addEventListener', args: ['onError'] }), event.origin);
    }
    if (data.event === 'onError') {
        results.set(player.videoId, false);
        players.delete(event.source as Window);
    }
});

/**
 * Starts listening to an embedded player's events (its src needs enablejsapi=1). Call it on the iframe's load event.
 * The handshake is repeated until the player answers, since it may not be ready right after loading.
 */
const watchPlayer = (iframe: HTMLIFrameElement, videoId: string) => {
    const target = iframe.contentWindow;
    if (!target) return;

    const player = { videoId, ready: false };
    players.set(target, player);
    let tries = 0;
    const listen = () => {
        if (player.ready || players.get(target) !== player || !iframe.isConnected || tries++ >= 20) return;
        target.postMessage(JSON.stringify({ event: 'listening', id: 1, channel: 'widget' }), '*');
        setTimeout(listen, 250);
    };
    listen();
};

// Embed URL that reports player errors to watchPlayer
const embedUrl = (videoId: string, autoplay = false) =>
    `https://www.youtube-nocookie.com/embed/${videoId}?enablejsapi=1&origin=${encodeURIComponent(window.location.origin)}${autoplay ? '&autoplay=1' : ''}`;

export const youtubeEmbedStore = {
    check,
    watchPlayer,
    embedUrl,
    // True when the video can be embedded, false when it can't, undefined while it's being checked
    isEmbeddable: (videoId: string) => results.get(videoId),
};
