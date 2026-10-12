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
- `TMDB_API_KEY` or `TMDB_ACCESS_TOKEN` (optional; used by the backend to enrich movie details and find YouTube trailers)

No real credentials belong in `.env.example`, source code, or documentation.

TMDB is optional during development. With neither backend credential configured, TMDB enrichment is disabled and the movie API uses its stored catalogue data. Credentials are read only by Spring Boot; never add them to frontend environment variables.

### Local Movie Posters

Place a verified JPEG poster at `frontend/public/images/posters/<movie-id>.jpg`, using the movie's `id` from the existing `/api/movies` response as the filename. For example, movie ID `21` uses `frontend/public/images/posters/21.jpg`. The UI tries this ID-specific local file first, then the movie's existing API poster URL, and uses the existing placeholder only if neither image can be loaded. This avoids title collisions and does not change the catalogue or database.

To enable current ratings, runtimes, credits, genres, language, and movie trailers, configure either a TMDB API key or a TMDB API Read Access Token in the backend process environment before starting Spring Boot. A v3 API key is sent as the `api_key` query parameter; a v4 read token is sent in the Authorization header. For example, in PowerShell:

```powershell
$env:TMDB_API_KEY = "your-tmdb-api-key"
# Alternatively, use a TMDB API Read Access Token:
# $env:TMDB_ACCESS_TOKEN = "your-tmdb-read-access-token"
.\gradlew.bat bootRun
```

Run that command from the `backend` folder. The credential is read only by Spring Boot and is never sent to the React application. When neither credential is set, the API returns available database values and leaves genuinely missing metadata or trailers empty; TMDB failures and rate limits also fall back to stored data. Movie lookup verifies the title and release year before requesting details and videos for the matched TMDB movie ID.

## Phase 1 Functionality

Phase 1 provides:

- Spring Boot backend foundation with environment-based MySQL configuration.
- `GET /api/health`, returning the backend status and service name.
- Explicit development CORS for `http://localhost:5173`.
- Responsive StreamAI landing page with original branding.
- Backend context and health endpoint tests.

Authentication, movie data, NLP, deep learning, recommendations, user interactions, and admin features are planned for later phases and are not implemented yet.

## Phase 2 Movie API

Phase 2 adds MySQL-backed movie metadata with genres, cast members, directors, explicit join tables, DTO responses, pagination, and development seed data. The available read endpoints are:

- `GET /api/movies`
- `GET /api/movies/{id}`
- `GET /api/movies/search?query=space`
- `GET /api/movies/genre/Sci-Fi`
- `GET /api/movies/trending`
- `GET /api/movies/popular`
- `GET /api/movies/top-rated`

Pagination accepts `page` and `size`; list endpoints also accept an allowlisted sort such as `sort=rating,desc`. Trending currently ranks featured movies before rating and release date. Popular currently uses rating and release date because watch-event popularity data does not exist yet. These are deterministic metadata-based definitions, not user-behavior metrics.

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
