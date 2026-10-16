// Toolbar popup: lists the feeds of the current page and adds them to Feedora,
// and enables the extension on the site Feedora runs on.

const statusEl = document.getElementById('status');
const feedsEl = document.getElementById('feeds');
const folderRow = document.getElementById('folder-row');
const folderSelect = document.getElementById('folder');

const setStatus = (text) => {
    statusEl.textContent = text;
    statusEl.hidden = !text;
};

// Runs in the page: the feeds it links to with <link rel="alternate">, or the page itself if it is a feed
function findFeeds() {
    const feeds = [];
    const add = (url, title) => {
        if (!url || feeds.some((feed) => feed.url === url)) return;
        feeds.push({ url, title: (title || '').trim() });
    };

    const root = document.documentElement ? document.documentElement.nodeName.toLowerCase() : '';
    if (root === 'rss' || root === 'feed' || root === 'rdf:rdf') {
        const title = document.querySelector('channel > title, feed > title');
        add(location.href, title ? title.textContent : '');
    }

    for (const link of document.querySelectorAll('link[rel~="alternate"][href]')) {
        if (/(rss|atom|rdf)\+xml/i.test(link.type || '')) {
            add(link.href, link.title);
        }
    }

    return feeds;
}

// Runs in a page: the Feedora session, if it is a Feedora tab (marked by content.js)
function readSession() {
    const root = document.documentElement;
    if (!(root instanceof HTMLElement) || !root.dataset.feedoraExtension) return null;

    const token = localStorage.getItem('auth_token');
    if (!token) return null;

    // Older app builds don't mark their API server, they serve it on the page's own origin
    return { server: root.dataset.feedoraServer || location.origin, token };
}

// The session of an open Feedora tab is the freshest; the stored one covers a closed Feedora tab
const findSession = async () => {
    const tabs = await chrome.tabs.query({});
    for (const tab of tabs) {
        // Tabs are listed with a URL only on the sites the extension has access to
        if (!tab.url || !/^https?:/.test(tab.url)) continue;
        try {
            const [injection] = await chrome.scripting.executeScript({ target: { tabId: tab.id }, func: readSession });
            if (injection && injection.result) {
                const session = { server: new URL(injection.result.server).origin, token: injection.result.token };
                await chrome.storage.local.set({ session });
                return session;
            }
        } catch {
            // Not allowed on that tab, or it is still loading
        }
    }

    const { session } = await chrome.storage.local.get('session');
    return session && session.token ? session : null;
};

// Calls the Feedora API with the session the content script got from the Feedora tab
const api = async (session, path, options = {}) => {
    const response = await fetch(session.server + path, {
        ...options,
        headers: { ...options.headers, Authorization: `Bearer ${session.token}` },
    });
    if (response.status === 401 || response.status === 403) {
        throw new Error('Your Feedora session has expired. Open Feedora, sign in and try again.');
    }
    return response;
};

const loadFolders = async (session) => {
    const response = await api(session, '/api/folders');
    if (!response.ok) return;

    const folders = await response.json();
    folders.sort((a, b) => a.name.localeCompare(b.name));
    for (const folder of folders) {
        folderSelect.add(new Option(folder.name, folder.id));
    }

    const { lastFolderId } = await chrome.storage.local.get('lastFolderId');
    if (lastFolderId && folders.some((folder) => folder.id === lastFolderId)) {
        folderSelect.value = lastFolderId;
    }
    folderRow.hidden = false;
};

folderSelect.addEventListener('change', () => {
    chrome.storage.local.set({ lastFolderId: folderSelect.value });
});

const renderFeed = (session, feed) => {
    const item = document.createElement('li');

    const title = document.createElement('span');
    title.className = 'feed-title';
    title.textContent = feed.title || new URL(feed.url).hostname;

    const url = document.createElement('span');
    url.className = 'feed-url';
    url.textContent = feed.url;
    url.title = feed.url;

    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = 'Add';
    button.disabled = !session;

    const result = document.createElement('span');
    result.className = 'feed-result';
    result.hidden = true;

    button.addEventListener('click', async () => {
        button.disabled = true;
        result.hidden = true;
        try {
            // No title, so Feedora reads it from the feed itself
            const response = await api(session, '/api/feed/addFeed', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ url: feed.url, folderId: folderSelect.value || null }),
            });
            const text = await response.text();
            result.textContent = text || (response.ok ? 'Feed added' : `Error ${response.status}`);
            result.className = `feed-result ${response.ok ? 'ok' : 'error'}`;
            button.textContent = response.ok ? 'Added' : 'Add';
            button.disabled = response.ok;
        } catch (error) {
            result.textContent = error instanceof TypeError
                ? `Could not reach Feedora at ${session.server}`
                : error.message;
            result.className = 'feed-result error';
            button.disabled = false;
        }
        result.hidden = false;
    });

    item.append(title, button, url, result);
    feedsEl.append(item);
};

const showFeeds = async (tab) => {
    let feeds = [];
    try {
        const [injection] = await chrome.scripting.executeScript({ target: { tabId: tab.id }, func: findFeeds });
        feeds = injection.result || [];
    } catch {
        setStatus('Feeds can\'t be read on this page.');
        return;
    }

    if (feeds.length === 0) {
        setStatus('No feed found on this page.');
        return;
    }

    let session = await findSession();
    let status = feeds.length === 1 ? '' : `${feeds.length} feeds found.`;
    if (!session) {
        status = 'Open Feedora in a tab and sign in to add a feed.';
    } else {
        try {
            await loadFolders(session);
        } catch (error) {
            status = error instanceof TypeError ? `Could not reach Feedora at ${session.server}` : error.message;
            session = null;
        }
    }

    setStatus(status);
    for (const feed of feeds) {
        renderFeed(session, feed);
    }
};

// Feedora on another server (e.g. Docker): background.js registers the content script for that site.
// The site permission alone doesn't say it, the browser can grant it for all sites.
const showEnable = async (tab) => {
    const url = new URL(tab.url);
    const enabledEl = document.getElementById('enabled');
    const enableButton = document.getElementById('enable');
    const disableButton = document.getElementById('disable');

    // Always on in the manifest
    if (url.hostname === 'localhost' || url.hostname === '127.0.0.1') {
        enabledEl.hidden = false;
        return;
    }

    // Match patterns can't have a port, so this covers every port on that host (like the localhost entries)
    const pattern = `${url.protocol}//${url.hostname}/*`;
    const id = `feedora-${url.protocol.slice(0, -1)}-${url.hostname}`;

    const showState = (enabled) => {
        enabledEl.hidden = !enabled;
        disableButton.hidden = !enabled;
        enableButton.hidden = enabled;
    };
    showState((await chrome.scripting.getRegisteredContentScripts({ ids: [id] })).length > 0);

    enableButton.addEventListener('click', () => {
        // If the permission prompt closes the popup (Firefox), background.js finishes it when the permission is added
        chrome.storage.session.set({ pendingEnable: pattern });
        // Must be called right away in the click handler, the browser only allows it during a user action
        chrome.permissions.request({ origins: [pattern] }).then(async (granted) => {
            if (!granted) return;
            await chrome.runtime.sendMessage({ type: 'FEEDORA_ENABLE_SITE', pattern });
            showState(true);
        }).catch((error) => console.error('Feedora Companion: could not enable on this site', error));
    });

    disableButton.addEventListener('click', async () => {
        await chrome.runtime.sendMessage({ type: 'FEEDORA_DISABLE_SITE', pattern });
        showState(false);
    });
};

(async () => {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab || !tab.url || !/^https?:/.test(tab.url)) {
        setStatus('Open a web page to look for its feed.');
        return;
    }

    showEnable(tab);
    await showFeeds(tab);
})();
