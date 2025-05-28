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

- **API Path Refactoring** 🔧
  - Moved FeedController from `/api` to `/api/feed` for better organization
  - Updated all frontend API calls to match new paths
  - Improved REST API structure and consistency

- **Internationalization** 🌍
  - Added 25+ new language entries for folder management
  - Enhanced English and Greek translations
  - Comprehensive error messages and UI labels

### Changed
- **Folder Interface:** Complete redesign with table layout, search, and actions
- **API Structure:** Reorganized endpoints for better separation of concerns
- **User Experience:** Modern Bootstrap styling with loading states and error handling

### Fixed
- Backend compilation issues (missing methods in UserService, FeedRepository)
- Language service integration with proper `Language.getString()` usage
- User authentication context for folder operations

### Technical Notes
- No database migration required
- Existing feeds remain unaffected
- Safe folder deletion with automatic feed migration

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
