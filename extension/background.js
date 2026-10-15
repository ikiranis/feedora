// Opens links requested by the Feedora page in a background tab next to the current one.

const MESSAGE_TYPE = 'FEEDORA_OPEN_BACKGROUND_TAB';

chrome.runtime.onMessage.addListener((message, sender) => {
    if (!message || message.type !== MESSAGE_TYPE) return;

    let url;
    try {
        url = new URL(message.url);
    } catch {
        return;
    }
    // Only open web links
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return;

    const tab = sender.tab;
    chrome.tabs.create({
        url: url.href,
        active: false,
        index: tab ? tab.index + 1 : undefined,
        openerTabId: tab ? tab.id : undefined,
        windowId: tab ? tab.windowId : undefined,
    });
});

// Clicking the toolbar button on a Feedora tab enables the extension on that site (e.g. Feedora in Docker on
// another server). The site permission is remembered, and the content script is registered for future visits.
chrome.action.onClicked.addListener((tab) => {
    let url;
    try {
        url = new URL(tab.url);
    } catch {
        return;
    }
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return;

    // Match patterns can't have a port, so this covers every port on that host (like the localhost entries)
    const pattern = `${url.protocol}//${url.hostname}/*`;

    // Must be called right away in the click handler, the browser only allows it during a user action
    chrome.permissions.request({ origins: [pattern] }).then(async (granted) => {
        if (!granted) return;

        const id = `feedora-${url.protocol.slice(0, -1)}-${url.hostname}`;
        const registered = await chrome.scripting.getRegisteredContentScripts({ ids: [id] });
        if (registered.length === 0) {
            await chrome.scripting.registerContentScripts([{
                id,
                matches: [pattern],
                js: ['content.js'],
                runAt: 'document_start',
                persistAcrossSessions: true,
            }]);
        }

        // Activate it in the current tab too, so there is no need to reload
        await chrome.scripting.executeScript({ target: { tabId: tab.id }, files: ['content.js'] });
        await chrome.action.setBadgeText({ tabId: tab.id, text: 'ON' });
    }).catch((error) => console.error('Feedora Companion: could not enable on this site', error));
});
