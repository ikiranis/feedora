# Feedora Companion

Browser extension that lets Feedora open posts in a **background tab** when you press **B** on a selected post.
Web pages can't open background tabs on their own, so the app hands the link to this extension.

Without the extension, B still works but the browser switches to the new tab.

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

If Feedora runs somewhere else (e.g. in Docker on `http://192.168.1.10:8820`), open it and **click the extension's
toolbar button** (pin it from the extensions menu first). Allow access to the site when the browser asks. The badge
shows **ON**, B works right away, and the site is remembered for next time (all ports on that host).

To disable it on a site, remove the extension's access to it in the browser's extension settings.

## How it works

1. `content.js` marks the page with `data-feedora-extension` on `<html>` so the app knows the extension is installed.
2. On B, the app posts `{ type: 'FEEDORA_OPEN_BACKGROUND_TAB', url }` with `window.postMessage`.
3. `content.js` forwards it to `background.js`, which opens the link with `chrome.tabs.create({ active: false })`
   right after the current tab.

Only `http`/`https` links are opened, and only messages posted by the page itself are accepted.
