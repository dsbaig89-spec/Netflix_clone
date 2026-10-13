# StreamFlix

An AI-powered, Netflix-inspired movie discovery and recommendation platform.

## Project Overview

StreamFlix is an educational full-stack project for exploring movie discovery, personalized experiences, NLP, and recommendation systems. It uses original StreamFlix branding and public/demo metadata only.

## Current Technology Stack

- Java 25
- Spring Boot 4.1.1
- Gradle 9.7.1
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- Spring Security (BCrypt, CSRF-protected cookie sessions)
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

The backend contains the movie catalogue, OMDb search and details integration, health endpoint, and cookie-based user authentication.

## Backend Setup

1. Install Java 25 and MySQL 8.
2. Create a local `backend/.env` file or export the database variables in your shell. Spring Boot imports `backend/.env` when started from the backend directory; operating-system environment variables take precedence. Never commit `.env`.
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

The frontend runs at `http://localhost:5173` by default; the backend CORS allowlist also supports `http://localhost:5174`.

## Environment Variables

Copy the values from `.env.example` into `backend/.env` or configure them as process environment variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SESSION_COOKIE_SECURE` (`false` for local HTTP; set to `true` behind HTTPS in production)
- `SESSION_COOKIE_SAME_SITE` (defaults to `Lax`; keep frontend and API same-site)
- `OMDB_API_KEY` (optional; used only by the backend to search OMDb and load movie details)
- `TMDB_API_KEY` or `TMDB_ACCESS_TOKEN` (optional; used only by the backend for catalogue poster/metadata enrichment and verified YouTube trailer discovery)
- `YOUTUBE_API_KEY` (optional; used only by the backend YouTube Data API v3 service for verified, embeddable trailer lookup)

No real credentials belong in `.env.example`, source code, or documentation.

OMDb and TMDB are optional during development. With no provider credentials configured, the existing database catalogue and local search continue to work. OMDb search endpoints return a safe configuration error; catalogue reads do not require either provider. Credentials are read only by Spring Boot; never add them to frontend environment variables.

### Local Movie Posters

Place a verified JPEG poster at `frontend/public/images/posters/<movie-id>.jpg`, using the movie's `id` from the existing `/api/movies` response as the filename. For example, movie ID `21` uses `frontend/public/images/posters/21.jpg`. The shared poster component tries the API-resolved poster, then this ID-specific local file, then the existing placeholder. A failed image load advances to the next source. This avoids title collisions and does not change the catalogue or database.

To enable OMDb title search and catalogue metadata/poster fallback, set `OMDB_API_KEY` in `backend/.env` or the backend process environment before starting Spring Boot. Catalogue enrichment looks up by title and release year, verifies the returned title/year, caches both hits and misses, and does not persist or overwrite existing movie records.

TMDB remains optional and provides catalogue poster/metadata enrichment and a fallback verified YouTube trailer source when configured. Set either `TMDB_API_KEY` or `TMDB_ACCESS_TOKEN` in `backend/.env` or the backend process environment. The backend requests TMDB's `/search/movie` using the release year, verifies an exact normalized title and year match, uses `poster_path` only when present, and constructs images with TMDB's supported `w500` image path.

YouTube Data API v3 trailer lookup is also optional. Set `YOUTUBE_API_KEY` only in `backend/.env` or in the backend process environment. `GET /api/movies/{id}/trailer` first preserves a valid stored/verified provider trailer, then searches using the catalogue title and release year, requests embeddable videos, checks the candidate title/year/trailer context, and ranks matching results instead of accepting the first result. It caches successful and temporarily unavailable lookups, avoids assigning one video to unrelated movie identities, and returns `available: false` if it cannot verify a trailer. YouTube credentials are never sent to React. The React trailer action opens the existing modal, shows lookup/unavailable status, and stops playback when closed. OMDb is never treated as a trailer source.

Spring loads the backend `.env` using `spring.config.import=optional:file:./.env[.properties]` in `application.properties`. This path is relative to the **backend process working directory**, not automatically relative to the repository or the properties file. Start Spring Boot from the `backend` directory (as in the commands below), or export `YOUTUBE_API_KEY` into the process environment. The app does not print the configured key.

Provider credentials must not be set in `frontend/.env` or any `VITE_*` variable. Do not commit real credentials.

For example, in PowerShell:

```powershell
$env:OMDB_API_KEY = "your-omdb-api-key"
.\gradlew.bat bootRun
```

Run that command from the `backend` folder. The credential is read only by Spring Boot and is never sent to the React application. OMDb is accessed through `GET /api/movies/omdb/search?query=...` and `GET /api/movies/omdb/{imdbId}`. Search returns title, year, IMDb ID, and poster when available; the details endpoint also returns plot, rating, runtime, and genres. OMDb does not provide YouTube trailer links. No trailer is invented for OMDb results.

The movie list and detail API responses expose the database movie ID, title, year, overview, rating, runtime, genres, poster URL, and a valid stored/provider YouTube trailer URL and video key when available. Genuine missing values remain null/omitted. Metadata is resolved for list, search, genre, and ranking responses as well as details, so home rows, search, details, and My List share one poster and trailer resolution path. Provider results are cached in process; existing database records and IDs are not changed.

The repository's isolated H2 test profile seeds 95 catalogue entries (20 illustrative demo entries and 75 curated titles). The curated poster mapping currently has 7 exact title/year entries; the seeded data contains no verified trailer mappings. These fixture counts are not a live MySQL inventory. A live database inventory requires starting the backend against that database; this implementation does not edit or delete its records.

## Phase 1 Functionality

Phase 1 provides:

- Spring Boot backend foundation with environment-based MySQL configuration.
- `GET /api/health`, returning the backend status and service name.
- Explicit development CORS for `http://localhost:5173` and `http://localhost:5174`.
- Responsive StreamFlix interface with original branding.
- Session-based registration, login, logout, current-user lookup, and CSRF protection.
- Backend context, authentication, and health endpoint tests.

NLP, deep learning, personalized recommendations, user interactions, and admin features remain planned.

## Authentication

The React app checks the current session on startup and requires authentication before rendering the movie catalogue. During Vite development, `/api` is proxied to Spring Boot so session and CSRF cookies remain same-origin, regardless of whether the frontend is opened at `localhost` or `127.0.0.1`. Set `VITE_API_BASE_URL` only when intentionally connecting directly to another API origin. The backend uses Spring Security sessions, BCrypt password hashes, an HttpOnly `STREAMFLIX_SESSION` cookie, and Spring's synchronizer-token CSRF protection. The browser never stores an authentication token in local storage. The "Remember me" option extends the session to 30 days and sets a persistent HttpOnly cookie; the default session lifetime is 30 minutes.

Available endpoints:

- `GET /api/auth/csrf` — initialize/read the CSRF token for the current session.
- `POST /api/auth/register` — create an account; duplicate email addresses return `409`.
- `POST /api/auth/login` — authenticate and establish the session.
- `GET /api/auth/me` — return the current user's safe profile; requires authentication.
- `POST /api/auth/logout` — invalidate the session; requires authentication and a CSRF token.

For `POST` requests, the frontend first obtains the CSRF token and sends it in the `X-CSRF-TOKEN` header with credentials. Movie catalogue and health endpoints remain public; other backend endpoints require authentication. Hibernate `ddl-auto=update` adds the `users` table without dropping existing movie tables or data. No JWT secret or additional auth secret is required.

For production, serve the frontend and backend over HTTPS, set `SESSION_COOKIE_SECURE=true`, restrict CORS to the deployed frontend origin, and supply database/OMDb credentials through the deployment environment or secret manager. The local CORS allowlist supports `http://localhost:5173` and `http://localhost:5174`.

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
