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
