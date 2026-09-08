# StreamAI

AI-Powered Netflix-Inspired Movie Recommendation Platform.

## Project Overview

StreamAI is an educational full-stack project for exploring movie discovery, personalized experiences, NLP, and recommendation systems. It uses original StreamAI branding and public/demo metadata only. AI and recommendation capabilities will be added in later phases.

## Current Technology Stack

- Java 25
- Spring Boot 4.1.1
- Gradle 9.7.1
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- MySQL Connector/J
- React 19
- Vite
- JavaScript

## Project Structure

```text
netflix-ai-clone/
├── backend/
│   └── src/
│       ├── main/java/com/netflixai/backend/
│       └── main/resources/
├── frontend/
│   ├── src/
│   └── package.json
├── docs/
├── .env.example
└── README.md
```

The backend package folders are prepared for later phases. They currently contain no feature implementations beyond the health endpoint.

## Backend Setup

1. Install Java 25 and MySQL 8.
2. Create a local `backend/.env` file or export the database variables in your shell. Never commit `.env`.
3. Create the database and application user in MySQL if they do not already exist.
4. Start the backend:

```powershell
cd backend
.\gradlew.bat bootRun
```

The backend runs at `http://localhost:8080`.

## Frontend Setup

```powershell
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`.

## Environment Variables

Copy the values from `.env.example` into your local environment as appropriate:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (reserved for a later authentication phase)
- `TMDB_API_KEY` (reserved for a later integration phase)

No real credentials belong in `.env.example`, source code, or documentation.

## Phase 1 Functionality

Phase 1 provides:

- Spring Boot backend foundation with environment-based MySQL configuration.
- `GET /api/health`, returning the backend status and service name.
- Explicit development CORS for `http://localhost:5173`.
- Responsive StreamAI landing page with original branding.
- Backend context and health endpoint tests.

Authentication, movie data, NLP, deep learning, recommendations, user interactions, and admin features are planned for later phases and are not implemented yet.

## Testing

Backend tests use an isolated in-memory H2 database profile:

```powershell
cd backend
.\gradlew.bat clean test
```

Build the frontend with:

```powershell
cd frontend
npm run build
```
