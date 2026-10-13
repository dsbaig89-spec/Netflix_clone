import { useEffect, useMemo, useState } from 'react'
import { getMovieById } from '../services/movieService.js'
import PosterImage from './PosterImage.jsx'

const genreNames = (movie) => {
  const rawGenres = movie?.genres ?? movie?.genre
  const values = Array.isArray(rawGenres) ? rawGenres : rawGenres ? [rawGenres] : []
  return values.map((genre) => typeof genre === 'string' ? genre : genre?.name).filter(Boolean)
}

const normalizeGenre = (genre) => {
  const normalized = String(genre || '').trim().toLowerCase().replace(/[-_]/g, ' ').replace(/\s+/g, ' ')
  return normalized === 'sci fi' || normalized === 'scifi' ? 'science fiction' : normalized
}

const displayValue = (value) => value || 'Not available'

export default function MovieDetailsModal({ movie, allMovies = [], open, trailerOpen, onClose, onPlay, onToggleMyList, isInMyList, onSelectSimilar }) {
  const [details, setDetails] = useState(movie)
  const [loading, setLoading] = useState(false)
  const [detailsError, setDetailsError] = useState('')

  useEffect(() => {
    if (!open || !movie?.id) return undefined

    setDetails(movie)
    if (movie.detailsLoaded) {
      setLoading(false)
      setDetailsError('')
      return undefined
    }

    const controller = new AbortController()
    setLoading(true)
    setDetailsError('')
    getMovieById(movie.id, { signal: controller.signal, imdbId: movie.omdbImdbId })
      .then((result) => {
        if (result) setDetails(result)
        else setDetailsError('Full details are unavailable. Showing the information we have.')
      })
      .catch((error) => {
        if (error.name !== 'AbortError') setDetailsError('Full details could not be loaded. Showing the information we have.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => controller.abort()
  }, [movie, open])

  useEffect(() => {
    if (!open) return
    const onKeyDown = (event) => {
      if (event.key === 'Escape' && !trailerOpen) onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open, trailerOpen, onClose])

  useEffect(() => {
    if (!open) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = previousOverflow }
  }, [open])

  const similarMovies = useMemo(() => {
    if (!details) return []
    const selectedGenres = new Set(genreNames(details).map(normalizeGenre))
    if (!selectedGenres.size) return []
    return allMovies
      .filter((candidate) => candidate.id !== details.id)
      .map((candidate) => ({
        movie: candidate,
        sharedGenres: genreNames(candidate).filter((genre) => selectedGenres.has(normalizeGenre(genre))).length,
      }))
      .filter((candidate) => candidate.sharedGenres > 0)
      .sort((first, second) => second.sharedGenres - first.sharedGenres || first.movie.title.localeCompare(second.movie.title))
      .slice(0, 6)
      .map(({ movie: candidate }) => candidate)
  }, [allMovies, details])

  if (!open || !movie) return null

  const cast = Array.isArray(details?.cast) ? details.cast : []
  const directors = Array.isArray(details?.directors) ? details.directors : []

  return (
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <dialog open className="movie-modal" aria-modal="true" aria-labelledby="movie-modal-title" onClick={(event) => event.stopPropagation()}>
        <button type="button" className="movie-modal__close" aria-label="Close movie details" onClick={onClose}>✕</button>
        <div className="movie-modal__backdrop" style={{ backgroundImage: details?.backdropUrl ? `url("${details.backdropUrl}")` : undefined }} />

        <div className="movie-modal__body">
          {loading ? <p className="movie-modal__status" role="status">Loading movie details...</p> : null}
          {detailsError ? <p className="movie-modal__status movie-modal__status--error" role="status">{detailsError}</p> : null}
          <div className="movie-modal__intro">
            <PosterImage className="movie-modal__poster" movie={details} alt={`${details?.title || movie.title} poster`} />
            <div className="movie-modal__main">
              <span className="movie-modal__eyebrow">{details?.demoContent ? 'Fictional demo title' : 'Movie details'}</span>
              <h2 id="movie-modal-title">{details?.title || movie.title}</h2>
              <div className="movie-modal__info-row">
                <span>{displayValue(details?.releaseYear)}</span>
                <span>{details?.runtime ? `${details.runtime} min` : 'Runtime not available'}</span>
                <span>{details?.rating !== null && details?.rating !== undefined ? `★ ${Number(details.rating).toFixed(1)}` : 'Rating not available'}</span>
                <span>{displayValue(details?.ageCertification)}</span>
              </div>
              <p className="movie-modal__overview">{details?.overview || 'Synopsis not available.'}</p>
              <div className="movie-modal__actions">
                <button type="button" className="movie-modal__button movie-modal__button--primary" onClick={() => onPlay(details)}>
                  ▶ Trailer
                </button>
                <button type="button" className="movie-modal__button movie-modal__button--secondary" onClick={() => onToggleMyList(details)}>
                  {isInMyList ? 'Remove from My List' : 'Add to My List'}
                </button>
              </div>
            </div>
          </div>

          <dl className="movie-modal__details">
            <div><dt>Genres</dt><dd>{genreNames(details).join(', ') || 'Not available'}</dd></div>
            <div><dt>Director</dt><dd>{directors.join(', ') || 'Not available'}</dd></div>
            <div><dt>Main cast</dt><dd>{cast.map((person) => typeof person === 'string' ? person : person?.name).filter(Boolean).join(', ') || 'Not available'}</dd></div>
            <div><dt>Language</dt><dd>{displayValue(details?.language)}</dd></div>
            {details?.demoContent ? <div><dt>Content note</dt><dd>Fictional demo content. Names, rating, and story are illustrative.</dd></div> : null}
          </dl>

          {similarMovies.length ? (
            <section className="similar-movies" aria-labelledby="similar-movies-title">
              <div className="similar-movies__header">
                <h3 id="similar-movies-title">Similar Movies</h3>
                <span>Genre-based similarity</span>
              </div>
              <div className="similar-movies__grid">
                {similarMovies.map((similar) => (
                  <button key={similar.id} className="similar-movies__item" type="button" onClick={() => onSelectSimilar(similar)}>
                    <PosterImage movie={similar} alt="" loading="lazy" />
                    <span>{similar.title}</span>
                  </button>
                ))}
              </div>
            </section>
          ) : null}
        </div>
      </dialog>
    </div>
  )
}