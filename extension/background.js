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

// The popup enables the extension on a site (e.g. Feedora in Docker on another server): the content script is
// registered for future visits and injected in the open tabs of that site, so there is no need to reload.
const siteId = (pattern) => {
    // Only the patterns the popup asks for, like http://192.168.1.10/*
    const match = /^(https?):\/\/([^/*]+)\/\*$/.exec(pattern);
    return match ? `feedora-${match[1]}-${match[2]}` : null;
};

const enableSite = async (pattern) => {
    const id = siteId(pattern);
    if (!id) return;

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
        try {
            await chrome.scripting.executeScript({ target: { tabId: tab.id }, files: ['content.js'] });
            await chrome.action.setBadgeText({ tabId: tab.id, text: 'ON' });
        } catch {
            // Tab still loading or not scriptable, the registered script covers it next time
        }
    }
};

const disableSite = async (pattern) => {
    const id = siteId(pattern);
    if (!id) return;

    const registered = await chrome.scripting.getRegisteredContentScripts({ ids: [id] });
    if (registered.length > 0) {
        await chrome.scripting.unregisterContentScripts({ ids: [id] });
    }

    const tabs = await chrome.tabs.query({ url: pattern });
    for (const tab of tabs) {
        await chrome.action.setBadgeText({ tabId: tab.id, text: '' });
    }
};

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    // Only from the popup (an extension page, not a tab)
    if (!message || sender.id !== chrome.runtime.id || sender.tab) return;

    const action = message.type === 'FEEDORA_ENABLE_SITE' ? enableSite
        : message.type === 'FEEDORA_DISABLE_SITE' ? disableSite : null;
    if (!action) return;

    chrome.storage.session.remove('pendingEnable');
    action(message.pattern)
        .catch((error) => console.error('Feedora Companion: could not change', message.pattern, error))
        .finally(() => sendResponse());
    return true;
});

// The permission prompt can close the popup (Firefox), then the site is enabled here. Only for the site the popup
// asked for: the permission is also added when the browser grants access to sites from its own menu.
chrome.permissions.onAdded.addListener(async ({ origins = [] }) => {
    const { pendingEnable } = await chrome.storage.session.get('pendingEnable');
    if (!pendingEnable || !origins.includes(pendingEnable)) return;

    await chrome.storage.session.remove('pendingEnable');
    enableSite(pendingEnable).catch((error) => console.error('Feedora Companion: could not enable on', pendingEnable, error));
});
