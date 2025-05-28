# Changelog

All notable changes to Feedora RSS Reader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
