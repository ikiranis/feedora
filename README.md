# feedora RSS Reader

feedora is a modern, full-stack RSS reader application. It features a Java 17+ Spring Boot backend and a Vue 3 + TypeScript frontend, providing a robust and extensible platform for reading and managing RSS feeds.

![feedora screenshot](https://github.com/user-attachments/assets/3ae2cbbb-2817-47fe-b62f-955138fbfd22)

## Features

- **Backend:** Java 17+, Spring Boot, RESTful API for feed management
- **Frontend:** Vue 3, TypeScript, Vite, Vue Router
- **RSS Feed Management:** Add, remove, and organize RSS feeds
- **Internationalization:** Language selection and multi-language support
- **Modern UI:** Responsive and user-friendly interface
- **Settings & Customization:** Personalize your reading experience

---

## Project Structure

```
feedora/
├── frontend/           # Vue 3 frontend app
│   ├── src/
│   ├── public/
│   └── ...
├── src/main/java/      # Java Spring Boot backend
│   └── ...
├── src/main/resources/ # Spring Boot resources
├── pom.xml             # Maven build file
└── ...
```

---

## Getting Started

### Prerequisites
- Java 17 or newer
- Maven
- Node.js (v16+ recommended)

### Backend (Spring Boot)

1. Navigate to the project root:
   ```sh
   cd /path/to/feedora
   ```
2. Build and run the backend:
   ```sh
   ./mvnw spring-boot:run
   ```
   The backend will start on `http://localhost:9999` by default.

### Frontend (Vue 3)

1. Navigate to the frontend directory:
   ```sh
   cd frontend
   ```
2. Install dependencies:
   ```sh
   npm install
   ```
3. Start the development server:
   ```sh
   npm run dev
   ```
   The frontend will be available at `http://localhost:8080` (or as indicated in the terminal).

---

## Customization
- **Feeds:** Add your own feed sources and categories in the frontend.
- **Backend:** Extend controllers, services, and models under `src/main/java/...` for new features.
- **Frontend:** Add new views and components in `frontend/src/`.
- **Internationalization:** Update language files in `src/main/resources/multiLanguage.xml` and the frontend language store.
- **Branding:** Update app titles, labels, and icons as needed.

---

## License
feedora is provided as a starting point for your own RSS reader projects. You may use, modify, and distribute it as needed.
