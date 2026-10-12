import { API_BASE_URL } from '../config.js'
import { getYouTubeVideoId } from '../utils/youtube.js'

const FALLBACK_BACKDROP = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1600&q=80'

const toArray = (value) => {
  if (Array.isArray(value)) return value
  return value ? [value] : []
}

const apiBaseUrl = () => import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? '' : API_BASE_URL)

const imageUrl = (value, baseUrl, pathBase = 'tmdb') => {
  if (typeof value !== 'string' || !value.trim()) return null
  const url = value.trim()
  if (/^https?:\/\//i.test(url)) return url
  if (url.startsWith('//')) return `https:${url}`
  if (url.startsWith('/')) {
    return pathBase === 'api'
      ? `${baseUrl}${url}`
      : `https://image.tmdb.org/t/p/w500${url}`
  }
  return url
}

const movieImageUrl = (movie, baseUrl, urlFields, pathFields) => {
  for (const field of urlFields) {
    const url = imageUrl(movie[field], baseUrl, 'api')
    if (url) return url
  }
  for (const field of pathFields) {
    const url = imageUrl(movie[field], baseUrl, 'tmdb')
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
    overview: movie.overview || movie.description || '',
    releaseDate,
    releaseYear,
    runtime: movie.runtime ?? null,
    rating: movie.rating === null || movie.rating === undefined || movie.rating === ''
      ? null
      : Number.isFinite(Number(movie.rating)) ? Number(movie.rating) : null,
    posterUrl: movieImageUrl(
      movie,
      apiBaseUrl(),
      ['posterUrl', 'poster_url', 'imageUrl', 'image_url', 'poster'],
      ['posterPath', 'poster_path']
    ),
    backdropUrl: movieImageUrl(
      movie,
      apiBaseUrl(),
      ['backdropUrl', 'backdrop_url', 'backdrop'],
      ['backdropPath', 'backdrop_path']
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

  try {
    const response = await fetch(`${baseUrl}/api/movies/search?query=${encodeURIComponent(cleanedQuery)}&page=0&size=12`, { signal })
    if (!response.ok) {
      throw new Error('Search failed')
    }

    const payload = await response.json()
    const results = extractMovies(payload)

    return { fallback: false, results }
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Search is unavailable because the movie backend could not be reached.')
  }
}

export async function getMovieById(movieId, { signal } = {}) {
  const baseUrl = apiBaseUrl()

  try {
    const response = await fetch(`${baseUrl}/api/movies/${movieId}`, { signal })
    if (!response.ok) {
      throw new Error('Failed to fetch movie details')
    }

    return normalizeMovie(await response.json())
  } catch (error) {
    if (error.name === 'AbortError') throw error
    return null
  }
}
