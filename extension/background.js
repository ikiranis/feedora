// Opens links requested by the Feedora page in a background tab next to the current one,
// and keeps the Feedora session that the popup uses to add feeds.

const MESSAGE_TYPE = 'FEEDORA_OPEN_BACKGROUND_TAB';
const SESSION_TYPE = 'FEEDORA_SESSION';

const isWebUrl = (value) => {
    try {
        const url = new URL(value);
        return url.protocol === 'http:' || url.protocol === 'https:';
    } catch {
        return false;
    }
};

chrome.runtime.onMessage.addListener((message, sender) => {
    if (!message) return;

    if (message.type === MESSAGE_TYPE) {
        // Only open web links
        if (typeof message.url !== 'string' || !isWebUrl(message.url)) return;

        const tab = sender.tab;
        chrome.tabs.create({
            url: new URL(message.url).href,
            active: false,
            index: tab ? tab.index + 1 : undefined,
            openerTabId: tab ? tab.id : undefined,
            windowId: tab ? tab.windowId : undefined,
        });
    }

    if (message.type === SESSION_TYPE) {
        // Only from the content script of a Feedora tab
        if (!sender.tab || sender.id !== chrome.runtime.id) return;
        if (typeof message.server !== 'string' || !isWebUrl(message.server)) return;

        const server = new URL(message.server).origin;
        const token = typeof message.token === 'string' && message.token ? message.token : null;
        chrome.storage.local.set({ session: { server, token } });
    }
});

// When the popup enables the extension on a site (e.g. Feedora in Docker on another server), the content script is
// registered for future visits and injected in the open tabs of that site, so there is no need to reload.
// Done here and not in the popup, because the permission prompt can close the popup.
chrome.permissions.onAdded.addListener(async ({ origins = [] }) => {
    for (const pattern of origins) {
        // Only the patterns the popup asks for, like http://192.168.1.10/*
        const match = /^(https?):\/\/([^/*]+)\/\*$/.exec(pattern);
        if (!match) continue;

        try {
            const id = `feedora-${match[1]}-${match[2]}`;
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

            const tabs = await chrome.tabs.query({ url: pattern });
            for (const tab of tabs) {
                await chrome.scripting.executeScript({ target: { tabId: tab.id }, files: ['content.js'] });
                await chrome.action.setBadgeText({ tabId: tab.id, text: 'ON' });
            }
        } catch (error) {
            console.error('Feedora Companion: could not enable on', pattern, error);
        }
    }
});
