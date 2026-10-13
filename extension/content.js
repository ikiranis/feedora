// Bridges the Feedora page and the extension background script.
// The page posts { type: 'FEEDORA_OPEN_BACKGROUND_TAB', url } and this script forwards it.

const MESSAGE_TYPE = 'FEEDORA_OPEN_BACKGROUND_TAB';

// Let the page know the extension is installed (the DOM is shared with the page).
document.documentElement.dataset.feedoraExtension = chrome.runtime.getManifest().version;

window.addEventListener('message', (event) => {
    // Only accept messages from the page itself
    if (event.source !== window || event.origin !== window.location.origin) return;

    const data = event.data;
    if (!data || data.type !== MESSAGE_TYPE || typeof data.url !== 'string') return;

    chrome.runtime.sendMessage({ type: MESSAGE_TYPE, url: data.url });
});
