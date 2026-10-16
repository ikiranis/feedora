# Feedora Companion

Browser extension that:

- lets Feedora open posts in a **background tab** when you press **B** on a selected post. Web pages can't open
  background tabs on their own, so the app hands the link to this extension. Without the extension, B still works
  but the browser switches to the new tab.
- **adds the feed of the page you're on** to Feedora from its toolbar popup.

## Install

### Chrome / Edge / Brave
1. Open `chrome://extensions` (or `edge://extensions`).
2. Enable **Developer mode**.
3. Click **Load unpacked** and select this `extension` folder.
4. Reload the Feedora tab.

### Firefox (128+)
1. Open `about:debugging#/runtime/this-firefox`.
2. Click **Load Temporary Add-on…** and select `manifest.json` in this folder.
3. Reload the Feedora tab.

Temporary add-ons are removed when Firefox restarts.

## Where it runs

The extension is always active on `http://localhost` and `http://127.0.0.1` (any port), which covers the Vite
dev server and the Spring Boot app on port 9999.

If Feedora runs somewhere else (e.g. in Docker on `http://192.168.1.10:8820`), open it, **click the extension's
toolbar button** (pin it from the extensions menu first) and choose **Enable the companion here**. Allow access to
the site when the browser asks. The badge shows **ON**, B works right away, and the site is remembered for next time
(all ports on that host).

To disable it on a site, remove the extension's access to it in the browser's extension settings.

## Adding a page's feed

1. Open Feedora and sign in once, so the extension knows your server and session.
2. On any site, click the toolbar button. The popup lists the feeds the page links to
   (`<link rel="alternate">` RSS/Atom), or the page itself if it is a feed.
3. Pick a folder (remembered for next time) and click **Add**. Feedora reads the feed's title itself.

If you sign out or the session expires, open Feedora and sign in again.

## How it works

1. `content.js` marks the page with `data-feedora-extension` on `<html>` so the app knows the extension is installed.
2. On B, the app posts `{ type: 'FEEDORA_OPEN_BACKGROUND_TAB', url }` with `window.postMessage`.
3. `content.js` forwards it to `background.js`, which opens the link with `chrome.tabs.create({ active: false })`
   right after the current tab.

Only `http`/`https` links are opened, and only messages posted by the page itself are accepted.

For adding feeds, the app marks `<html>` with `data-feedora-server` (its API URL). On a Feedora tab, `content.js`
sends that URL and the login token (`auth_token` in the page's localStorage) to `background.js` when the page loads
and when you leave the tab; it is kept in the extension's own storage. The popup first reads the session straight
from an open Feedora tab (falling back to the page's origin on app builds without `data-feedora-server`), then finds the page's feeds with
`chrome.scripting.executeScript` and calls `/api/folders` and `/api/feed/addFeed` with that token.
