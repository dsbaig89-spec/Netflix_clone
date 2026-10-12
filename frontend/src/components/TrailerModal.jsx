import { useEffect } from 'react'
import { getYouTubeVideoId } from '../utils/youtube.js'

export default function TrailerModal({ movie, open, onClose }) {
  const videoId = getYouTubeVideoId(movie?.youtubeId) || getYouTubeVideoId(movie?.trailerUrl)

  useEffect(() => {
    if (!open) return undefined
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, onClose])

  if (!open || !movie) return null

  return (
    <div className="trailer-modal-backdrop" onClick={onClose} role="presentation">
      <section className="trailer-modal" role="dialog" aria-modal="true" aria-labelledby="trailer-modal-title" onClick={(event) => event.stopPropagation()}>
        <header className="trailer-modal__header">
          <h2 id="trailer-modal-title">{movie.title} Trailer</h2>
          <button type="button" className="trailer-modal__close" aria-label="Close trailer" onClick={onClose}>×</button>
        </header>
        {videoId ? (
          <div className="trailer-modal__player-wrap">
            <iframe
              className="trailer-modal__player"
              src={`https://www.youtube.com/embed/${videoId}?autoplay=1&playsinline=1&rel=0`}
              title={`${movie.title} official trailer`}
              allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
              allowFullScreen
            />
          </div>
        ) : (
          <p className="trailer-modal__unavailable" role="status">
            No verified YouTube trailer was found for this title. If you expected one, check that TMDB credentials are configured on the backend.
          </p>
        )}
      </section>
    </div>
  )
}