# Changelog

All notable changes to Feedora RSS Reader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.2] - 2025-05-29

### Added
- **Complete Folder Management System** 🗂️
  - **Frontend Components:**
    - New `AddFolderModal.vue` component with Bootstrap modal integration
    - Completely redesigned `Folder.vue` page with modern table layout
    - Search functionality for filtering folders by name
    - Real-time feed count display for each folder
    - Delete functionality with confirmation dialogs
    - Loading states and comprehensive error handling
    - Empty state illustrations and user-friendly messages
  
  - **Backend API Endpoints:**
    - `POST /api/addFolder` - Create new folders with duplicate checking
    - `DELETE /api/deleteFolder/{folderId}` - Delete folders with feed migration
    - Enhanced `GET /api/folders` - List all folders for current user
    - User-based folder isolation for security
  
  - **Backend Services:**
    - `FolderService.addFolder()` - Business logic for folder creation
    - `FolderService.deleteFolder()` - Safe folder deletion with feed migration
    - Transaction management for data consistency
    - Integration with internationalization system
  
  - **Database Enhancements:**
    - Added `findByFolder()` method to `FeedRepository`
    - Enhanced folder-feed relationship management
    - Automatic feed migration to default folder on folder deletion

- **Internationalization Improvements** 🌍
  - Added 25+ new language entries for folder management
  - English and Greek translations for all folder-related messages
  - Error messages, UI labels, and user feedback in multiple languages
  - Enhanced language support for:
    - Folder creation and deletion workflows
    - Search and filtering operations
    - Confirmation dialogs and error states
    - Loading and empty state messages

- **User Experience Enhancements** ✨
  - **Modern UI Design:**
    - Professional table layout for folder management
    - Bootstrap 5 integration for consistent styling
    - Responsive design for mobile and desktop
    - Interactive hover states and visual feedback
  
  - **Advanced Functionality:**
    - Real-time search with instant filtering
    - Feed count badges for quick overview
    - Confirmation dialogs for destructive actions
    - Loading spinners and progress indicators
    - Comprehensive error handling with user-friendly messages

- **Developer Improvements** 🔧
  - Enhanced API error handling and validation
  - Comprehensive JSDoc documentation
  - Type-safe TypeScript interfaces
  - Consistent error message patterns
  - Transaction management for data integrity

### Changed
- **Folder.vue Complete Redesign:**
  - Transformed from simple list to professional table interface
  - Added search functionality and action buttons
  - Improved accessibility and mobile responsiveness
  - Enhanced error handling and user feedback

- **FolderController Enhancements:**
  - Updated endpoint mappings from `/api/folders` to `/api`
  - Added proper request/response handling
  - Integrated user authentication context
  - Enhanced error handling and validation

- **Language System Integration:**
  - Fixed `LanguageService` usage in backend services
  - Proper integration with `Language.getString()` method
  - Consistent error message localization

### Fixed
- **Backend Compilation Issues:**
  - Added missing `getCurrentUser()` method to `UserService`
  - Fixed `LanguageService.get()` method references
  - Added missing `findByFolder()` method to `FeedRepository`
  - Resolved import issues and method signatures

- **User Context Management:**
  - Implemented proper user authentication for folder operations
  - Added user-based folder access control
  - Fixed folder ownership validation

### Technical Details
- **Frontend Stack:** Vue 3, TypeScript, Bootstrap 5, Vite
- **Backend Stack:** Java 21, Spring Boot 3.4.4, JPA/Hibernate
- **New Dependencies:** Enhanced modal and form handling
- **Database Changes:** New repository methods for folder-feed relationships
- **API Changes:** New REST endpoints for folder management

### Migration Notes
- All existing feeds will continue to work without folder assignment
- Folder deletion safely migrates feeds to default folder (no data loss)
- New language entries are automatically loaded from `multiLanguage.xml`
- No database migration required - JPA handles schema updates

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
