# Changelog

All notable changes to Feedora RSS Reader will be documented in this file.

## [0.11.8] - 2026-10-19

### Added
- **Favorites** ⭐ (#23)
  - Posts can be marked as favorites from the newsfeed, the posts view and the post modal
  - Favorite counts added to the user statistics
- **Mobile Experience** 📱 (#6)
  - Responsive app layout for small screens
  - Swipe navigation between posts in `PostModal`, plus pinch-to-zoom
  - Improved mobile search
- **"Remember me" on Login** 🔐
  - Optional long-lived session (default 30 days, configurable with `jwt.remember-me-expiration`); regular logins still expire after `jwt.expiration`
- **Very Busy Feeds:** feeds with more than 30 posts per hour are now checked every minute, with a new "Every minute" label

### Changed
- Restyled the new posts badge
- `PostModal` title alignment and responsiveness improvements
- Marking a whole folder as read hides it from the sidebar until it gets new unread posts (#24)
- Folders are refreshed after every scan, so empty folders reappear when they get unread posts

### Fixed
- A failed background folder refresh no longer empties the sidebar
- An older unread counts response can no longer overwrite a newer one

---

## [0.10.9] - 2026-10-18

### Added
- **Live Newsfeed** 📡 (#18)
  - New live newsfeed view that refreshes automatically, with a configurable refresh interval setting
- **OPML Export** 📤 (#22): export the user's feeds and folders as OPML
- **Tweet Support** 🐦
  - Better rendering of tweets in `PostElement`
  - Link previews for links in tweets (new link fields on `Post`)
  - Tweet photos are displayed
- **Thumbnails:** thumbnail extraction for posts (new `thumbnail` field on `Post`)
- **Mobile Sidebar** (#6): theme toggle and menu controls in the mobile sidebar

### Changed
- Feed operation status tracking and scan progress visibility improvements
- Session management: expired sessions are handled more cleanly, with better authentication error messages

---

## [0.9.20] - 2026-10-17

### Added
- **YouTube API Key Support:** optional YouTube Data API key for more reliable live video checks
- **Feedora Companion 1.2.0** 🧩
  - New popup that finds the feeds of the current page and adds them to Feedora
  - Session management in the extension, new icons and SVG logo
- **Search** (#7): improved search handling in the posts view
- **Mark as Unread** (#8): posts can be marked as unread again
- **Orphan Feeds** (#10): feeds without a folder are shown in the posts view
- **Hide YouTube Shorts per Folder:** Shorts can be hidden for selected folders, and are left out of their unread counts
- **Keyboard Shortcuts Modal** (#13) ⌨️
- **Feed Posts** (#15): show all posts of a single feed
- **Open in Modal** (#16): posts open in `PostModal`
- **Video Duration** (#17): YouTube video duration is displayed
- **Copy Link** (#14): copy a post's link
- **Scan Progress Bar** (#20) with feed operation status tracking
- **Targeted Parsing:** scan only a folder or a single feed
- **New Posts Count** (#21): counts unread posts added after the list was loaded

### Changed
- Sidebar collapsed state is persisted in localStorage (#12)
- Marking as read records when the post was marked (#19)
- Scrollbar styling and general styling updates
- YouTube embed and error handling improvements

---

## [0.8.14] - 2026-10-16

### Added
- **YouTube Live Streams & Premieres** 🔴
  - `LiveBadge` component showing the live status of videos
  - `YouTubeLiveChecker` utility and `liveStatus` / `liveStartAt` fields on `Post`
  - Periodic refresh of live video states
- **Folder Management in Posts View:** folders are edited from the posts view with new folder modals, replacing the separate Folders page
- **Feed Statistics Modal**, and a feed dropdown menu for editing, viewing statistics and unfollowing
- **HTTP Failure Indicators** for feeds, with feed log counts
- Router links for feed titles, and the selected feed title shown in the posts view
- Posts link in the sidebar with current page indication
- App version shown in the UI
- **Feedora Companion 1.1.0:** updated Firefox version requirement and site permission handling

### Changed
- `ArticleExtractor` uses HTTP/2 with improved fetch logic
- Batched inserts when saving posts
- Sidebar collapse and language select styling
- Backend URL handling in the frontend config
- Security configuration permits frontend files in subfolders

### Removed
- Separate Folders page/component (replaced by folder modals in the posts view)

---

## [0.7.1] - 2026-10-15

### Added
- **User Roles & Registration Control** (#2) 👥
  - `Role` enum (`ADMIN`, `USER`); the first registered user becomes admin
  - `registrationEnabled` setting to turn off public registration
  - Only admins can change roles
  - `EnumCheckConstraintCleanup` drops stale enum constraints on startup
- **Background Parsing for OPML Imports:** imported feeds are parsed in the background

---

## [0.6.3] - 2026-10-15

### Added
- **User Management:** create, edit and delete users with `UserModal`, plus per-user statistics
- **Post Cleanup:** read posts are deleted daily after `feedora.posts.read-retention-days` (default 30); saved posts are always kept
- Last parsed time displayed in the posts view
- Posts per day shown for feeds; problematic feeds are sorted first
- Broken images are handled gracefully in posts and the modal

---

## [0.4.0] - 2026-10-15

### Added
- **Feed Statistics** 📊
  - `FeedStatistics` model tracking parsing statistics per feed
  - Each feed is checked according to its own posting frequency
  - New log types for statistics errors

---

## [0.3.0] - 2026-10-15

### Added
- **Unread / All Filter:** the posts API can include read posts, with a UI toggle
- **Unit and API Tests** 🧪 for services, utilities and APIs (in-memory Derby, `ApiTestBase`, `TestHttpServer`)
- Favicon and apple-touch-icon
- New action buttons and close button on posts

### Changed
- Post titles open their link with modifier keys (Ctrl/Cmd-click)
- Improved unread tracking and marking as read on scroll
- Selecting the current filter reloads the posts
- Better post selection scrolling in list mode
- Improved layout and metadata in `PostElement` and `PostModal`
- Feed parsing handles feeds shared by several users, with a feed existence check

---

## [0.2.0] - 2026-10-14

### Added
- **Logging System** 📝
  - `LogService`, `LogEntry` and `LogController` for feed and post issues
  - Logging for fetch, parse, save and content extraction errors
  - Configurable log retention
  - `LogsModal` to view, filter and delete log entries per feed and per post
  - Translations for the log UI

---

## [0.1.0] - 2026-10-13

### Added
- **Reading Experience** 📖
  - Keyboard navigation and selection highlighting for posts
  - Expanded details section for the selected post, with media
  - Site icons in post titles
  - Full article content extraction, with preloading and caching
  - Lead image support and content sanitization
  - `PostModal` for full post content
  - Mark all as read
  - View modes saved in localStorage
- **YouTube** ▶️
  - Video embedding and playback in posts
  - `YouTubeFeedResolver` turns YouTube channel/playlist URLs into RSS feed URLs
- **Saved Posts:** save posts and manage the saved list
- **Folder & Feed Filtering** with unread counts in the sidebar
- Option to show/hide empty folders
- **Feedora Companion** browser extension for opening posts in background tabs

### Changed
- Page size increased to 50, with better infinite scroll
- Clicking a selected post toggles its selection
- Updated `ArticleExtractor` user agent and logging configuration
- Spring Boot updated to 3.4.13

### Fixed
- `.gitignore` covers macOS files

---

## [0.0.3] - 2025-05-30

### Added
- **Complete User Authentication System** 🔐
  - JWT-based authentication with secure token management
  - User registration and login functionality with form validation
  - `AuthController` with login, register, and logout endpoints
  - `SecurityConfig` for Spring Security integration
  - `JwtAuthenticationFilter` for request authentication
  - `JwtUtil` for token generation and validation
  - Protected routes with authentication guards
  - User session management with `authStore`
  - Modern login and registration pages with responsive design
  - Authentication state persistence across browser sessions

- **Enhanced Error Handling System** ⚠️
  - Improved `errorStore` with better state management
  - Comprehensive error handling in Post.vue component
  - Language initialization fallback system
  - Global error clearing on successful authentication
  - Router-level error state management
  - Better error messages for authentication failures

- **User Interface Improvements** ✨
  - Enhanced user section in sidebar with avatar and profile display
  - User information display with username and email
  - Logout functionality with confirmation
  - Improved visual consistency across authentication flows
  - Better styling for user interactions and feedback

- **Theme System Enhancement** 🌙
  - **Dark mode set as default theme** for better user experience
  - Simplified theme initialization logic
  - Improved theme persistence across sessions
  - Enhanced dark mode styling consistency

- **Multilingual Support Expansion** 🌍
  - Added comprehensive authentication-related translations
  - New language entries for login, registration, and user management
  - Enhanced multilingual support for feed management operations
  - Updated sign-up link text for consistency
  - Improved language initialization with proper fallbacks

### Changed
- **Authentication Flow:** Complete overhaul from admin-based to user-based authentication
- **Default Theme:** Dark mode is now the default for new users
- **Error Handling:** Simplified and more robust error management system
- **User Experience:** Enhanced feedback and loading states
- **Component Organization:** Better separation of authentication components
- **API Security:** All endpoints now require proper user authentication

### Fixed
- **Authentication Context:** Fixed controllers to use authenticated user instead of admin user
- **Post Fetching Errors:** Resolved "Failed to fetch posts" error after successful login
- **Language Initialization:** Fixed NullPointerException in Language configuration
- **Error State Persistence:** Cleared error states on successful navigation
- **Theme Initialization:** Improved theme loading and persistence logic
- **User Context:** Fixed all API calls to use current authenticated user

### Security Improvements
- **JWT Token Security:** Secure token generation with configurable secrets
- **Password Security:** Proper password hashing and validation
- **Route Protection:** All application routes now require authentication
- **Session Management:** Secure session handling with automatic cleanup
- **API Protection:** All backend endpoints protected with JWT authentication

### Technical Improvements
- **Dependencies:** Added Spring Security and JWT dependencies
- **Configuration:** New security configuration with JWT filter integration
- **Database:** Enhanced User entity with authentication fields
- **Frontend State:** Improved state management with authentication store
- **API Structure:** Consistent user context across all endpoints
- **Error Resilience:** Better error handling and recovery mechanisms

### Migration Notes
- **Breaking Change:** Application now requires user authentication
- **First Run:** Users need to register before accessing the application
- **Data Migration:** Existing data will be associated with the first registered user
- **Configuration:** New JWT secret configuration required in application properties

---

## [0.0.2] - 2025-05-29

### Added
- **Complete Folder Management System** 🗂️
  - New `AddFolderModal.vue` component for creating folders
  - Redesigned `Folder.vue` with modern table layout and search functionality
  - Backend API endpoints: `POST /api/addFolder`, `DELETE /api/deleteFolder/{id}`
  - User-based folder isolation and feed migration on deletion
  - Real-time feed count display per folder

- **Feed Management Enhancements** 📰
  - Feed update functionality with `EditFeedModal.vue` component
  - Search functionality for feeds by title
  - Organized feed modals into dedicated `/feed` directory
  - Feed parsing interval optimization (5→10 minutes)

- **Post Management Features** 📝
  - Search functionality for posts by title/description
  - Enhanced post styling with unread/read visual indicators
  - Improved post layout with folder name display
  - Better alignment and badge system

- **Theme System** 🎨
  - Complete light/dark theme toggle functionality
  - `themeStore` for persistent theme management
  - Dark mode support across all components
  - Enhanced styling for better visibility in both themes
  - Multilingual theme labels

- **API Path Refactoring** 🔧
  - Moved FeedController from `/api` to `/api/feed` for better organization
  - Updated all frontend API calls to match new paths
  - Improved REST API structure and consistency
  - Added `updateFeed` endpoint

- **Internationalization Improvements** 🌍
  - Added 25+ new language entries for folder management
  - Enhanced English and Greek translations
  - Multilingual support for search and filter functionality
  - Comprehensive error messages and UI labels

- **User Experience Enhancements** ✨
  - Modern Bootstrap styling with responsive design
  - Loading states and comprehensive error handling
  - Visual feedback for user interactions
  - Professional table layouts and search interfaces

### Changed
- **Component Organization:** Restructured feed-related components into `/feed` directory
- **Folder Interface:** Complete redesign with table layout, search, and actions
- **API Structure:** Reorganized endpoints for better separation of concerns
- **Post Styling:** Enhanced visual hierarchy with better typography and colors
- **Theme Integration:** Consistent theming across all components
- **Search Experience:** Improved placeholders and better UX for filtering

### Fixed
- Backend compilation issues (missing methods in UserService, FeedRepository)
- Language service integration with proper `Language.getString()` usage
- User authentication context for folder operations
- Dark theme styling inconsistencies
- Post element layout and alignment issues

### Technical Improvements
- **Performance:** Optimized feed parsing intervals
- **Code Organization:** Better component structure and separation of concerns
- **Type Safety:** Enhanced TypeScript interfaces and error handling
- **State Management:** Implemented theme store and improved data flow
- **Documentation:** Updated README with deployment instructions

### Technical Notes
- No database migration required
- Existing feeds remain unaffected
- Safe folder deletion with automatic feed migration
- Backward compatible API changes

---

## [0.0.1] - Initial Release

### Added
- Basic RSS feed management
- Post reading and marking functionality
- OPML import/export support
- Multi-threaded feed parsing
- Vue.js frontend with TypeScript
- Spring Boot backend with Derby database
- Initial folder structure (basic functionality)
- User management system
- Internationalization framework (English/Greek)
- REST API endpoints for feeds and posts
- Responsive web interface
- Automatic feed parsing scheduler
