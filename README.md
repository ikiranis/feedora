# Feedora RSS Reader

Feedora is a modern, full-stack RSS reader application that provides a seamless experience for managing and reading RSS feeds. Built with enterprise-grade technologies, it features a robust Java Spring Boot backend and a responsive Vue.js frontend.

![28-05--2025_00-45](https://github.com/user-attachments/assets/2fd9f8f7-6266-4f0d-81cc-6983d307d637)

## ✨ Key Features

### 🔧 **Technical Stack**
- **Backend:** Java 21, Spring Boot 3.4.4, JPA/Hibernate, Apache Derby
- **Frontend:** Vue 3, TypeScript, Vite, Bootstrap 5, Vue Router
- **Build Tools:** Maven, Vite
- **Database:** Apache Derby (embedded)

### 📰 **RSS Management**
- **Feed Operations:** Add, remove, and organize RSS feeds with folder support
- **OPML Support:** Import and export feed collections in OPML format
- **Multi-threaded Parsing:** Efficient feed fetching with up to 8 concurrent threads
- **Automatic Updates:** Scheduled feed parsing every 5 minutes
- **Duplicate Detection:** Prevents duplicate posts from the same feed

### 👤 **User Experience**
- **Post Management:** Mark posts as read/unread, pagination support
- **Folder Organization:** Organize feeds into custom folders
- **Responsive Design:** Modern, mobile-friendly interface
- **Internationalization:** Multi-language support with XML-based configuration
- **Search & Filter:** Find posts and feeds quickly

### 🛠️ **Developer Features**
- **RESTful API:** Well-documented endpoints for all operations
- **JPA Repositories:** Clean data access layer with custom queries
- **Operation Locking:** Prevents conflicts during OPML imports and feed parsing
- **Error Handling:** Comprehensive error management and logging

---

## 📁 Project Structure

```
feedora/
├── src/main/java/              # Spring Boot backend
│   └── eu/apps4net/feedora/
│       ├── controllers/        # REST API endpoints
│       ├── models/            # JPA entities (Post, Feed, User, Folder)
│       ├── repositories/      # Data access layer
│       ├── services/          # Business logic
│       ├── configurations/    # App configuration & scheduling
│       └── utilities/         # Helper classes (RSS fetching)
├── src/main/resources/        # Application resources
│   ├── application.properties # Database & server configuration
│   └── multiLanguage.xml     # Internationalization
├── frontend/                  # Vue 3 frontend application
│   ├── src/
│   │   ├── api/              # API client services
│   │   ├── components/       # Vue components
│   │   ├── views/            # Page components
│   │   ├── router/           # Vue Router configuration
│   │   └── types/            # TypeScript type definitions
│   ├── public/               # Static assets
│   └── package.json          # Frontend dependencies
├── db/                       # Derby database files
├── pom.xml                   # Maven configuration
└── README.md                 # This file
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 21 or newer** (Oracle JDK or OpenJDK)
- **Maven 3.6+** (or use included Maven wrapper)
- **Node.js 16+** with npm
- **Git** (for cloning the repository)

### 🔧 Quick Setup

#### 1. Clone & Navigate
```bash
git clone <repository-url>
cd feedora
```

#### 2. Backend Setup (Spring Boot)
```bash
# Using Maven wrapper (recommended)
./mvnw clean install
./mvnw spring-boot:run

# Or using system Maven
mvn clean install
mvn spring-boot:run
```

**Backend will start on:** `http://localhost:9999`

#### 3. Frontend Setup (Vue 3)
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

**Frontend will be available on:** `http://localhost:8080` (or as shown in terminal)

### 🌐 Application Access

1. **Frontend Interface:** http://localhost:8080
2. **Backend API:** http://localhost:9999/api
3. **Default Admin User:**
   - Email: `rocean@error.gr`
   - Password: `111111`

---

## 📋 API Endpoints

### Feed Management
- `GET /api/getAllFeeds` - Get all feeds
- `GET /api/getFeeds?page=1&pageSize=10` - Get paginated feeds
- `POST /api/addFeed` - Add a new RSS feed
- `POST /api/deleteFeed?feedId={id}` - Delete a feed
- `GET /api/fetchFeedInfo?url={rss-url}` - Get feed metadata
- `POST /api/importOPML` - Import feeds from OPML file
- `GET /api/feedOperationStatus` - Check feed operation status

### Post Management
- `GET /api/posts/getAllPosts` - Get all posts
- `GET /api/posts/getPosts?page=1&pageSize=10` - Get paginated posts
- `PUT /api/posts/markAsRead/{postId}` - Mark post as read
- `POST /api/posts/parseFeeds` - Manually trigger feed parsing
- `DELETE /api/posts/deleteAll` - Delete all posts

### User Management
- `GET /api/users` - Get all users

---

## 🔧 Development & Customization

### Backend Development
- **Add New Endpoints:** Create controllers in `src/main/java/eu/apps4net/feedora/controllers/`
- **Business Logic:** Implement services in `src/main/java/eu/apps4net/feedora/services/`
- **Data Models:** Define entities in `src/main/java/eu/apps4net/feedora/models/`
- **Database Queries:** Extend repositories in `src/main/java/eu/apps4net/feedora/repositories/`

### Frontend Development
- **New Components:** Add Vue components in `frontend/src/components/`
- **Pages:** Create new views in `frontend/src/views/`
- **API Integration:** Extend services in `frontend/src/api/`
- **Routing:** Configure routes in `frontend/src/router/`
- **Styling:** Customize styles in SCSS files

### Configuration
- **Database:** Modify `src/main/resources/application.properties`
- **Languages:** Update `src/main/resources/multiLanguage.xml`
- **Feed Parsing:** Adjust interval in `FeedParseWorkerConfig.java` (default: 5 minutes)
- **Thread Pool:** Configure in `PostService.java` (default: up to 8 threads)

### Building for Production
```bash
# Build backend
./mvnw clean package

# Build frontend
cd frontend
npm run build

# The built frontend will be in frontend/dist/
# The backend JAR will be in target/
```

---

## 🛠️ Technical Details

### Database Schema
- **Users:** Store user accounts and authentication
- **Folders:** Organize feeds into categories
- **Feeds:** RSS feed metadata and URLs
- **Posts:** Individual articles from feeds
- **Settings:** Application configuration

### Performance Features
- **Connection Pooling:** Efficient database connections
- **Lazy Loading:** JPA lazy loading for better performance
- **Pagination:** Built-in pagination for large datasets
- **Concurrency:** Multi-threaded feed parsing
- **Caching:** Built-in Spring caching where applicable

### Security Features
- **Input Validation:** Server-side validation for all inputs
- **SQL Injection Prevention:** JPA/Hibernate parameter binding
- **Error Handling:** Comprehensive error management
- **Operation Locking:** Prevents concurrent feed operations

---

## 📄 License

Feedora is provided as a starting point for your own RSS reader projects. You may use, modify, and distribute it as needed.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

### Development Workflow
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

---

## 🏗️ Architecture Overview

Feedora follows a clean architecture pattern with clear separation of concerns:

- **Presentation Layer:** Vue.js frontend with TypeScript
- **API Layer:** Spring Boot REST controllers
- **Business Layer:** Service classes with business logic
- **Data Layer:** JPA repositories and entities
- **Database:** Apache Derby for data persistence

The application uses reactive programming patterns where appropriate and implements proper error handling throughout the stack.
