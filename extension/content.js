// Bridges the Feedora page and the extension background script.
// The page posts { type: 'FEEDORA_OPEN_BACKGROUND_TAB', url } and this script forwards it.
// It also passes the Feedora server and login token to the extension, so the popup can add feeds.

const MESSAGE_TYPE = 'FEEDORA_OPEN_BACKGROUND_TAB';
const SESSION_TYPE = 'FEEDORA_SESSION';

// The script can be injected twice in the same page (toolbar button, or a site that is also in the manifest),
// which would open every link twice
// Not in XML pages (e.g. a feed opened in the browser), they have no HTML element to mark
if (!window.feedoraCompanionLoaded && document.documentElement instanceof HTMLElement) {
    window.feedoraCompanionLoaded = true;

    // Let the page know the extension is installed (the DOM is shared with the page).
    document.documentElement.dataset.feedoraExtension = chrome.runtime.getManifest().version;

    window.addEventListener('message', (event) => {
        // Only accept messages from the page itself
        if (event.source !== window || event.origin !== window.location.origin) return;

        const data = event.data;
        if (!data || data.type !== MESSAGE_TYPE || typeof data.url !== 'string') return;

        chrome.runtime.sendMessage({ type: MESSAGE_TYPE, url: data.url });
    });

    // Sent when the page loads and when the tab is left (e.g. after signing in, to go add a feed).
    // The app marks <html> with its API server; older builds don't, and serve the API on the page's own origin.
    const syncSession = () => {
        const marked = document.documentElement.dataset.feedoraServer;
        const token = localStorage.getItem('auth_token');
        if (!marked && !token) return;

        try {
            chrome.runtime.sendMessage({ type: SESSION_TYPE, server: marked || window.location.origin, token });
        } catch {
            // The extension was reloaded, this old copy of the script can't reach it any more
        }
    };

    if (document.readyState === 'complete') {
        syncSession();
    } else {
        window.addEventListener('load', syncSession);
    }
    document.addEventListener('visibilitychange', syncSession);
}
