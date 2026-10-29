# Feedora RSS Reader

Feedora is a self-hosted, multi-user RSS reader. It follows your RSS/Atom feeds and YouTube channels, checks each feed as often as it actually posts, and gives you a fast, keyboard-friendly reading view with full article extraction, saved posts, favorites and a live newsfeed. It runs as a single Java application with an embedded database, so there is nothing else to install or manage.

<img width="2784" height="1680" alt="CleanShot 2026-10-19 at 19 32 01@2x" src="https://github.com/user-attachments/assets/9140eb92-ca5a-4bfe-a928-f64bd740ec58" />

- [📄 Changelog](CHANGELOG.md)
- [🚀 Installation](#-installation)
- [⚙️ Configuration](#️-configuration)
- [🧩 Feedora Companion extension](#-feedora-companion-browser-extension)
- [📋 API reference](#-api-reference)

---

## ✨ What Feedora Does

### 📰 Feeds and Folders
- **Add any feed** by URL. Feedora reads the feed's title itself.
- **YouTube channels and playlists:** paste a normal YouTube URL and it is turned into the channel's RSS feed.
- **Folders:** organise feeds into folders, rename or delete folders, and show or hide empty ones. Feeds without a folder are shown too.
- **OPML import and export:** bring your subscriptions from another reader (imported feeds are parsed in the background) or export them.
- **Feed management:** edit a feed's title and folder, unfollow it, or open its posts, all from a dropdown on each feed.

### 🔄 Smart Feed Updates
- Feeds are checked in the background, **each one according to how often it posts**:

  | Feed activity | Checked every |
  |---|---|
  | More than 30 posts per hour | 1 minute |
  | More than 10 posts per hour | 5 minutes |
  | More than 1 post per hour | 10 minutes |
  | More than 1 post per day | 1 hour |
  | Up to 1 post per day | 6 hours |

- Up to 8 feeds are fetched in parallel, and duplicate posts are skipped.
- **Manual scans** of everything, a single folder or a single feed, with a progress bar.
- **Feed statistics** for each feed: posts per day, check interval, last and next check, and failures. Feeds with problems are listed first and marked with an HTTP failure indicator.
- **Logs** of fetch, parse and extraction errors per feed and per post, with filtering and deletion. Old log entries are removed automatically.

### 📖 Reading
- **List and card view modes**, infinite scrolling, and filtering by folder or feed, with **unread / all** toggles and unread counts in the sidebar.
- **Full article extraction:** the article text and lead image are pulled from the original page, cleaned up, and preloaded for the next posts.
- **Post modal** with the full content, swipe navigation between posts and pinch-to-zoom on mobile.
- Posts are **marked as read as you scroll past them**. You can also mark all as read, or mark a post as unread again.
- A **new posts badge** shows how many unread posts arrived since the list was loaded.
- **Search** across post titles and descriptions.
- **Saved posts** (read later) and **favorites**. Saved posts are never cleaned up.
- **Copy link** and share actions on each post.
- **Live newsfeed** page that refreshes on its own at a configurable interval. New posts are shown one at a time, 5 seconds apart (2 seconds when more than 50 are waiting), and the next check waits until they have all been shown.

### ▶️ YouTube and Social Content
- **Embedded YouTube playback** with video duration.
- **Live streams and premieres** are detected and shown with a live badge, and their state is refreshed. An optional YouTube Data API key makes this more reliable.
- **Hide YouTube Shorts** per folder.
- **Tweets** are rendered with their photos and link previews.
- **Thumbnails** are extracted for posts that don't include one.

### ⌨️ Keyboard Shortcuts
| Key | Action |
|---|---|
| `←` / `→` | Select the previous / next post |
| `↑` / `↓` | Select the post above / below, or scroll in list mode and in the opened post |
| `Enter` / `O` | Open / close the selected post |
| `Esc` | Close the opened post |
| `B` | Open the selected post's link in a background tab (best with the Companion extension) |
| `F` | Save / unsave the selected post |
| `S` | Add / remove the selected post from favorites |
| `C` | Copy the selected post's link |
| `R` | Show the new posts, when the new posts badge is shown |
| `H` | Show / hide the shortcuts |

### 👥 Users and Administration
- **Multi-user:** every user has their own feeds, folders and posts.
- **Login with JWT**, with an optional "remember me" that keeps you signed in for 30 days.
- **Roles:** the first registered user becomes **admin**. Admins can add, edit and delete users, change roles, turn public registration on or off, and edit application settings.
- **User statistics:** feeds, folders, posts, saved posts and favorites per user.
- **Automatic cleanup:** read posts are deleted after a retention period (30 days by default).

### 🎨 Interface
- Responsive layout for desktop, tablet and mobile, with a mobile sidebar.
- Dark mode by default, with a light theme toggle.
- English and Greek, set up in `multiLanguage.xml`.
- The app version is shown in the UI.

---

## 🔧 Technical Stack

- **Backend:** Java 21, Spring Boot 3.4, Spring Security (JWT), JPA/Hibernate
- **Database:** Apache Derby (embedded, stored in the `db/` folder)
- **Feed and content processing:** ROME, Jsoup, Readability4J
- **Frontend:** Vue 3, TypeScript, Vite, Bootstrap 5, Vue Router, Sass
- **Browser extension:** Manifest V3 (Chrome, Edge, Brave, Firefox 128+)

---

## 🚀 Installation

There are two ways to install Feedora:
- **[Download and run](#option-1-download-and-run)** the ready-made jar from the GitHub releases. This is the easiest way.
- **[Build from source](#option-2-build-from-source)**, if you want to change the code or run the latest unreleased version.

Then follow the [first steps](#first-steps).

### Option 1: Download and run

#### Requirements
**Java 21** or newer. Check with `java -version` in a terminal. If you don't have it, install it from [Adoptium](https://adoptium.net/) (Temurin, version 21 or newer).

#### 1. Download
Download `Feedora-x.y.z.jar` from the [latest release](https://github.com/ikiranis/feedora/releases/latest).

#### 2. Put it in its own folder
Create a folder for Feedora (e.g. `Feedora` in your Documents) and move the jar there. **Feedora keeps all its data in the folder of the jar**, so don't leave it in your Downloads folder.

#### 3. Double-click the jar
A small Feedora window opens and shows:

| | |
|---|---|
| **Status** | `Starting...`, then `Running`, or the error if it couldn't start (e.g. the port is in use) |
| **Version** | The running version |
| **Address** | The address on this computer, `http://localhost:9990`. Click it or **Open in browser** |
| **Network** | The address other devices of your network (phone, tablet, other computers) can use, e.g. `http://192.168.1.20:9990` |
| **Data folder** | The folder where the data is kept (the folder of the jar) |

Feedora runs while the window is open. **Quit**, or closing the window, stops it.

If the double-click doesn't start it:
- **Windows:** if the jar opens with a zip program, right-click it, choose **Open with → Choose another app** and select **Java(TM) Platform SE binary**. If Windows asks about the firewall, allow access to use Feedora from other devices.
- **macOS:** the first time, right-click the jar and choose **Open**, then **Open** again, as the jar is not signed by Apple. The window is available on Apple Silicon Macs (M1 and newer). On Intel Macs Feedora runs without it, so start it from a terminal instead (see below).
- **Linux:** right-click the jar and choose **Open with** your Java runtime. You may need to allow it to run as a program first (Properties → Permissions).

#### Running from a terminal
You can also start Feedora from a terminal, e.g. on a server:

```bash
java -jar Feedora-x.y.z.jar
```

From a terminal there is no window, Feedora keeps its data in the **current folder** (not the folder of the jar), and you stop it with `Ctrl+C`. Add `--gui` to show the window anyway, or `--no-gui` to never show it.

#### The files of Feedora
Feedora creates these next to the jar on its first start:

| File / folder | What it is |
|---|---|
| `db/` | The database: users, feeds, posts and settings |
| `application-secrets.properties` | The secret key that signs the logins, created automatically. Keep it private: anyone who has it can log in as any user |
| `feeds/` | The RSS feeds made from web pages on the **Parser** page |
| `derby.log` | The database log |

#### Updating
Quit Feedora, replace the jar with the new one in the same folder, and start it again. Your data stays as it is. It's a good idea to [back up](#backups) the folder first.

### Option 2: Build from source

#### Prerequisites
- **Java 21** or newer (JDK)
- **Node.js 18+** with npm, needed to build the frontend
- **Git**
- Maven is optional, because the included Maven wrapper (`./mvnw`) downloads it for you

#### 1. Clone the repository
```bash
git clone https://github.com/ikiranis/feedora.git
cd feedora
```

#### 2. Create the secrets file (optional)
The JWT secret lives in a file that is excluded from Git and from the jar. If you skip this step, Feedora creates a random secret on its first start and saves it in `application-secrets.properties` in the folder it runs from. To set your own, create the file from the template:

```bash
cp src/main/resources/application-secrets.properties.template \
   src/main/resources/application-secrets.properties

# Generate a secure secret
openssl rand -base64 64
```

Open `src/main/resources/application-secrets.properties` and paste the generated value into `jwt.secret`. The secret must be at least 256 bits; the 64-byte key above is more than enough.

> ⚠️ Never commit `application-secrets.properties`. Changing `jwt.secret` signs out every user.

#### 3. Build the frontend
```bash
cd frontend
npm install
npm run build
cd ..
```

This creates `frontend/dist/`. The Maven build copies it into the application, so the backend serves the frontend itself.

#### 4. Build and run the application
```bash
./mvnw clean package
java -jar target/Feedora-x.y.z.jar
```

The JAR name follows the version in `pom.xml`. On Windows, use `mvnw.cmd` instead of `./mvnw`.

Open **http://localhost:9990**.

The jar can also be copied to its own folder and started with a double-click, like the [released jar](#3-double-click-the-jar). Don't double-click it inside `target/`, as its data would be created there and deleted by `./mvnw clean`.

### First steps

#### 1. Create the first account
Click **Sign up** and register. **The first user becomes the admin.** If you don't want other people to register, turn off `registrationEnabled` in **Settings** afterwards and add users yourself from the **Users** page.

#### 2. Add your feeds
Add feeds one by one from the **Feeds** page, or import an OPML file exported from another reader. Feedora starts fetching them right away.

---

## 🛠️ Development Setup

Run the backend and the Vite dev server side by side to get hot reload:

```bash
# Terminal 1 – backend on http://localhost:9990
./mvnw spring-boot:run

# Terminal 2 – frontend with hot reload
cd frontend
npm install
npm run dev
```

Open the URL Vite prints (usually **http://localhost:5173**). In development mode the frontend calls the backend at `http://localhost:9990`, and in production it uses the address it was served from.

### Running the tests
```bash
./mvnw test
```

The tests use an in-memory Derby database and disable the background workers, so your `db/` data is not touched.

---

## ⚙️ Configuration

### `application.properties`
File: `src/main/resources/application.properties`

| Property | Default | Description |
|---|---|---|
| `server.port` | `9990` | HTTP port |
| `spring.datasource.url` | `jdbc:derby:db;create=true;` | Derby database. The `db/` folder is created in the working directory on first run |
| `feedora.logs.retention-days` | `30` | Days to keep feed/post log entries |
| `feedora.posts.read-retention-days` | `30` | Days to keep read posts after they were read. Saved posts are always kept |

Any property can also be passed when starting the app, for example:
```bash
java -jar target/Feedora-x.y.z.jar --server.port=8080
```

### `application-secrets.properties`
| Property | Default | Description |
|---|---|---|
| `jwt.secret` | Generated | Base64 key, at least 256 bits. When it's not set, Feedora creates one on its first start and saves it in `application-secrets.properties` in its data folder |
| `jwt.expiration` | `86400000` (24 h) | Session length in milliseconds |
| `jwt.remember-me-expiration` | `2592000000` (30 days) | Session length when "remember me" is checked |

### In-app settings (admin only)
On the **Settings** page:

| Setting | Description |
|---|---|
| `registrationEnabled` | Allow new users to sign up (`true`/`false`) |
| `youtubeApiKey` | Optional [YouTube Data API](https://developers.google.com/youtube/v3) key for checking live and scheduled videos. When it is empty, Feedora reads the video pages instead |
| `liveNewsfeedRefreshSeconds` | How often the Live Newsfeed page refreshes |
| `defaultActionsLanguage` | Default language for messages |

### Backups
All data is in the `db/` folder. Stop the application and copy that folder, together with `application-secrets.properties`, to back it up. Without the secrets file the backup still works, but everyone has to log in again.

---

## 🧩 Feedora Companion (Browser Extension)

The `extension/` folder contains an optional browser extension that:

- opens posts in a **background tab** when you press **B**, which web pages can't do on their own
- **saves the page you're on** to Feedora's **Saved** posts from its toolbar popup
- **adds the feed of the page you're on** to Feedora from its toolbar popup

**Chrome / Edge / Brave:** open `chrome://extensions`, enable **Developer mode**, click **Load unpacked** and select the `extension` folder.

**Firefox 128+:** open `about:debugging#/runtime/this-firefox`, click **Load Temporary Add-on…** and select `extension/manifest.json`. Temporary add-ons are removed when Firefox restarts.

The extension works automatically on `localhost` and `127.0.0.1`. If Feedora runs on another address, open Feedora, click the extension's toolbar button and choose **Enable the companion here**. See [extension/README.md](extension/README.md) for details.

---

## 📁 Project Structure

```
feedora/
├── src/main/java/eu/apps4net/feedora/
│   ├── configurations/   # Security, JWT filter, CORS, background workers (feed parsing, post cleanup)
│   ├── controllers/      # REST API
│   ├── exceptions/       # API exceptions and global handler
│   ├── models/           # JPA entities and DTOs (User, Feed, Folder, Post, FeedStatistics, LogEntry, Setting)
│   ├── repositories/     # Spring Data repositories
│   ├── services/         # Business logic
│   └── utilities/        # RSS fetching, article/thumbnail/link preview extraction, YouTube helpers, JWT
├── src/main/resources/
│   ├── application.properties
│   ├── application-secrets.properties.template
│   └── multiLanguage.xml # UI and message translations (en, el)
├── src/test/             # Unit and API tests
├── frontend/             # Vue 3 + TypeScript app
│   └── src/
│       ├── api/          # API clients
│       ├── components/   # Post, modals, sidebar, feed/folder/user/log components
│       ├── views/        # Posts, Live newsfeed, Feeds, Users, Logs, Settings, Login/Register
│       ├── router/
│       └── functions/    # Stores, config, helpers
├── extension/            # Feedora Companion browser extension
├── db/                   # Derby database (created on first run, not in Git)
└── pom.xml
```

---

## 📋 API Reference

All endpoints are under `/api`. Everything except `/api/auth/**`, `/api/language/**` and `/api/general/**` needs an `Authorization: Bearer <token>` header. Endpoints marked 🔒 are for admins only.

### Authentication – `/api/auth`
| Method | Path | Description |
|---|---|---|
| GET | `/status` | Whether registration is enabled |
| POST | `/register` | Register a user |
| POST | `/login` | Log in (`email`, `password`, `rememberMe`) and get a token |
| GET | `/me` | Current user |
| POST | `/logout` | Log out |

### Feeds – `/api/feed`
| Method | Path | Description |
|---|---|---|
| GET | `/getAllFeeds`, `/getFeeds` | All feeds / paginated feeds |
| POST | `/addFeed` | Add a feed |
| POST | `/updateFeed` | Update a feed's title and folder |
| POST | `/deleteFeed` | Unfollow a feed |
| GET | `/fetchFeedInfo` | Read a feed's metadata from its URL |
| POST | `/importOPML` | Import an OPML file |
| GET | `/exportOPML` | Export feeds as OPML |
| GET | `/feedOperationStatus` | Status of running scans and imports |
| GET | `/lastParsedAt` | Time of the last scan |

### Folders – `/api`
| Method | Path | Description |
|---|---|---|
| GET | `/folders` | The user's folders |
| POST | `/addFolder` | Create a folder |
| PUT | `/updateFolder/{folderId}` | Rename a folder |
| DELETE | `/deleteFolder/{folderId}` | Delete a folder |

### Posts – `/api/posts`
| Method | Path | Description |
|---|---|---|
| GET | `/getPosts` | Paginated posts (filters: search, folder, feed, saved, include read, hidden Shorts) |
| GET | `/getAllPosts` | All posts |
| GET | `/unreadCounts` | Unread counts per folder and feed |
| GET | `/countNew` | Unread posts added since a given time |
| GET | `/live`, `/live/refreshSeconds` | Live newsfeed and its refresh interval |
| GET | `/content/{postId}` | Extracted full article content |
| PUT | `/markAsRead/{postId}`, `/markAsUnread/{postId}` | Change a post's read state |
| PUT | `/markAllAsRead` | Mark all (or a folder's / feed's) posts as read |
| PUT | `/saved/{postId}`, `/favorite/{postId}` | Toggle saved / favorite |
| POST | `/parseFeeds` | Start a scan (all, a folder or a feed) |
| DELETE | `/deleteAll` | Delete all posts |

### Users – `/api/users`
| Method | Path | Description |
|---|---|---|
| GET | `/` | Users with statistics |
| POST | `/` 🔒 | Add a user |
| PUT | `/{userId}` | Update a user |
| PUT | `/{userId}/role` 🔒 | Change a user's role |
| DELETE | `/{userId}` 🔒 | Delete a user |

### Other
| Method | Path | Description |
|---|---|---|
| GET / DELETE | `/api/logs` | Read / delete log entries |
| GET / PUT | `/api/setting/...` 🔒 | Read / update settings |
| GET | `/api/language/all/{language}` | Translations |
| GET | `/api/general/appAlive`, `/api/general/version` | Health check and app version |

---

## 🤝 Contributing

Issues, feature requests and pull requests are welcome.

1. Fork the repository
2. Create a feature branch
3. Make your changes and run `./mvnw test`
4. Open a pull request

## 📄 License

Feedora is provided as a starting point for your own RSS reader projects. You may use, modify and distribute it as needed.
