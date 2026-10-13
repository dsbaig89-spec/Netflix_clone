import { API_BASE_URL } from '../config.js'
import { getYouTubeVideoId } from '../utils/youtube.js'

const FALLBACK_BACKDROP = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1600&q=80'

const toArray = (value) => {
  if (Array.isArray(value)) return value
  return value ? [value] : []
}

const apiBaseUrl = () => import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? '' : API_BASE_URL)

const imageUrl = (value, baseUrl) => {
  if (typeof value !== 'string' || !value.trim()) return null
  const url = value.trim()
  if (/^https?:\/\//i.test(url)) return url
  if (url.startsWith('//')) return `https:${url}`
  if (url.startsWith('/')) return `${baseUrl}${url}`
  return url
}

const movieImageUrl = (movie, baseUrl, urlFields) => {
  for (const field of urlFields) {
    const url = imageUrl(movie[field], baseUrl)
    if (url) return url
  }
  return null
}

const normalizeMovie = (movie) => {
  const releaseDate = movie.releaseDate || movie.release_date || null
  const rawReleaseYear = movie.releaseYear ?? movie.release_year
  const releaseYear = Number.isFinite(Number(rawReleaseYear)) && rawReleaseYear !== null && rawReleaseYear !== ''
    ? Number(rawReleaseYear)
    : releaseDate
      ? new Date(releaseDate).getFullYear()
      : null

  const rawGenres = movie.genres ?? movie.genre
  const genres = (Array.isArray(rawGenres) ? rawGenres : toArray(rawGenres))
    .map((genre) => typeof genre === 'string' ? genre : genre?.name)
    .filter(Boolean)

  return {
    id: movie.id,
    title: movie.title || 'Untitled Movie',
    overview: movie.overview || movie.plot || movie.description || '',
    omdbImdbId: movie.imdbId || movie.imdbID || null,
    releaseDate,
    releaseYear,
    runtime: movie.runtime ?? null,
    rating: movie.rating === null || movie.rating === undefined || movie.rating === ''
      ? null
      : Number.isFinite(Number(movie.rating)) ? Number(movie.rating) : null,
    posterUrl: movieImageUrl(
      movie,
      apiBaseUrl(),
      ['posterUrl', 'poster_url', 'imageUrl', 'image_url', 'poster']
    ),
    backdropUrl: movieImageUrl(
      movie,
      apiBaseUrl(),
      ['backdropUrl', 'backdrop_url', 'backdrop']
    ) || FALLBACK_BACKDROP,
    trailerUrl: typeof movie.trailerUrl === 'string' && movie.trailerUrl.trim() ? movie.trailerUrl.trim() : typeof movie.trailer_url === 'string' && movie.trailer_url.trim() ? movie.trailer_url.trim() : typeof movie.videoUrl === 'string' && movie.videoUrl.trim() ? movie.videoUrl.trim() : typeof movie.video_url === 'string' && movie.video_url.trim() ? movie.video_url.trim() : '',
    youtubeId: getYouTubeVideoId(movie.youtubeId || movie.youtube_id || movie.trailerUrl || movie.trailer_url || movie.videoUrl || movie.video_url),
    language: movie.language || '',
    country: movie.country || '',
    ageCertification: movie.ageCertification || movie.age_certification || '',
    demoContent: Boolean(movie.demoContent ?? movie.demo_content),
    featured: Boolean(movie.featured),
    detailsLoaded: Array.isArray(movie.cast) && Array.isArray(movie.directors),
    genres,
    cast: Array.isArray(movie.cast) ? movie.cast : [],
    directors: Array.isArray(movie.directors) ? movie.directors : [],
  }
}

const extractMovies = (payload) => {
  const raw = payload && payload.content ? payload.content : payload && Array.isArray(payload.results) ? payload.results : payload && Array.isArray(payload.items) ? payload.items : Array.isArray(payload) ? payload : []
  return Array.isArray(raw) ? raw.map(normalizeMovie) : []
}

const fetchMoviePage = async (baseUrl, page, signal) => {
  const url = `${baseUrl}/api/movies?page=${page}&size=100&sort=title,asc`
  const response = await fetch(url, { signal })
  if (!response.ok) throw new Error(`Movie catalogue request failed with HTTP ${response.status}.`)
  return response.json()
}

const fetchAllMovies = async (baseUrl, signal) => {
  const firstPage = await fetchMoviePage(baseUrl, 0, signal)
  if (Array.isArray(firstPage)) return firstPage.map(normalizeMovie)

  const firstMovies = extractMovies(firstPage)
  const totalPages = Number(firstPage?.totalPages ?? firstPage?.page?.totalPages ?? 1)
  const remainingPages = await Promise.all(
    Array.from({ length: Math.max(0, totalPages - 1) }, (_, index) => fetchMoviePage(baseUrl, index + 1, signal))
  )
  const movies = [...firstMovies, ...remainingPages.flatMap(extractMovies)]
  const uniqueMovies = new Map()
  movies.forEach((movie) => {
    if (movie.id !== null && movie.id !== undefined) uniqueMovies.set(String(movie.id), movie)
  })
  return [...uniqueMovies.values()]
}

export async function fetchCatalog({ signal } = {}) {
  const baseUrl = apiBaseUrl()
  const movies = await fetchAllMovies(baseUrl, signal)
  if (!movies.length) throw new Error('The backend returned an empty movie catalogue.')

  return {
    fallback: false,
    movies,
    rows: {
      trending: movies.slice(0, 8),
      popular: movies.slice(4, 12),
      topRated: [...movies].sort((a, b) => Number(b.rating) - Number(a.rating)).slice(0, 8),
      action: movies.filter((movie) => movie.genres.some((genre) => ['Action', 'Adventure'].includes(genre))).slice(0, 8),
      indian: movies.filter((movie) => movie.language === 'Hindi' || movie.country === 'India').slice(0, 8),
      sciFi: movies.filter((movie) => movie.genres.some((genre) => ['Sci-Fi', 'Science Fiction'].includes(genre))).slice(0, 8),
      comedy: movies.filter((movie) => movie.genres.includes('Comedy')).slice(0, 8),
      myList: [],
    },
    featuredMovie: movies.find((movie) => movie.featured) || movies[0],
  }
}

export async function searchMovies(query, { signal } = {}) {
  const cleanedQuery = String(query || '').trim()
  const baseUrl = apiBaseUrl()

  if (!cleanedQuery) {
    return { fallback: false, results: [] }
  }

  const localSearch = async () => {
    const response = await fetch(`${baseUrl}/api/movies/search?query=${encodeURIComponent(cleanedQuery)}&page=0&size=12`, { signal })
    if (!response.ok) throw new Error(`Catalogue search failed with HTTP ${response.status}.`)
    return extractMovies(await response.json())
  }

  const omdbSearch = async () => {
    const response = await fetch(`${baseUrl}/api/movies/omdb/search?query=${encodeURIComponent(cleanedQuery)}&page=1`, { signal })
    if (!response.ok) {
      const payload = await response.json().catch(() => null)
      throw new Error(payload?.message || `OMDb search failed with HTTP ${response.status}.`)
    }
    const payload = await response.json()
    return (Array.isArray(payload.results) ? payload.results : []).map((movie) => normalizeMovie({
      ...movie,
      id: movie.imdbId,
      overview: movie.plot,
      detailsLoaded: false,
    }))
  }

  const [localResult, omdbResult] = await Promise.allSettled([localSearch(), omdbSearch()])
  if (signal?.aborted) {
    const abortError = new Error('The operation was aborted.')
    abortError.name = 'AbortError'
    throw abortError
  }
  const localMovies = localResult.status === 'fulfilled' ? localResult.value : []
  const omdbMovies = omdbResult.status === 'fulfilled' ? omdbResult.value : []
  if (localResult.status === 'rejected' && omdbResult.status === 'rejected') {
    throw new Error(omdbResult.reason?.message || localResult.reason?.message || 'Movie search is unavailable.')
  }

  const results = []
  const seen = new Set()
  for (const movie of [...localMovies, ...omdbMovies]) {
    const key = `${movie.title.trim().toLowerCase()}|${movie.releaseYear || ''}`
    if (!seen.has(key)) {
      seen.add(key)
      results.push(movie)
    }
  }

  const notices = []
  if (omdbResult.status === 'rejected') notices.push('OMDb search is unavailable; showing catalogue matches only.')
  if (localResult.status === 'rejected') notices.push('The StreamFlix catalogue search is unavailable.')
  return { fallback: false, results, notice: notices.join(' ') }
}

export async function getMovieById(movieId, { signal, imdbId } = {}) {
  const baseUrl = apiBaseUrl()

  try {
    const path = imdbId
      ? `/api/movies/omdb/${encodeURIComponent(imdbId)}`
      : `/api/movies/${movieId}`
    const response = await fetch(`${baseUrl}${path}`, { signal })
    if (!response.ok) {
      const payload = await response.json().catch(() => null)
      throw new Error(payload?.message || `Failed to fetch movie details (HTTP ${response.status}).`)
    }

    const payload = await response.json()
    return normalizeMovie(imdbId
      ? { ...payload, overview: payload.plot, cast: [], directors: [], detailsLoaded: true }
      : payload)
  } catch (error) {
    if (error.name === 'AbortError') throw error
    return null
  }
}

export async function getMovieTrailer(movieId, { signal } = {}) {
  const baseUrl = apiBaseUrl()
  const response = await fetch(`${baseUrl}/api/movies/${encodeURIComponent(movieId)}/trailer`, { signal })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || `Trailer lookup failed with HTTP ${response.status}.`)
  }
  return response.json()
}
