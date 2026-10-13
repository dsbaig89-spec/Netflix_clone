export const POSTER_PLACEHOLDER = '/poster-placeholder.svg'

export const getPosterSources = (movie) => {
  const movieId = movie?.id
  const hasMovieId = (typeof movieId === 'number' && Number.isFinite(movieId))
    || (typeof movieId === 'string' && movieId.trim() !== '')
  const localPoster = hasMovieId
    ? `/images/posters/${encodeURIComponent(String(movieId))}.jpg`
    : null
  const existingPoster = typeof movie?.posterUrl === 'string'
    && movie.posterUrl.trim()
    && movie.posterUrl !== POSTER_PLACEHOLDER
    ? movie.posterUrl
    : null

  return [...new Set([existingPoster, localPoster, POSTER_PLACEHOLDER].filter(Boolean))]
}
