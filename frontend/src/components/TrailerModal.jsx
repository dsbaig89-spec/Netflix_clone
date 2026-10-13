import { useEffect } from 'react'
import { getYouTubeVideoId } from '../utils/youtube.js'

export default function TrailerModal({ movie, open, loading = false, error = '', onClose }) {
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
        {loading ? (
          <p className="trailer-modal__unavailable" role="status">Finding a verified, embeddable trailer…</p>
        ) : error ? (
          <p className="trailer-modal__unavailable" role="status">{error}</p>
        ) : videoId ? (
          <>
            <div className="trailer-modal__player-wrap">
              <iframe
                className="trailer-modal__player"
                src={`https://www.youtube.com/embed/${videoId}?autoplay=1&playsinline=1&rel=0`}
                title={`${movie.title} trailer`}
                allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
                allowFullScreen
              />
            </div>
            <p className="trailer-modal__unavailable" role="status">
              If this video is unavailable or embedding is disabled, <a href={`https://www.youtube.com/watch?v=${videoId}`} target="_blank" rel="noreferrer">open it on YouTube</a>.
            </p>
          </>
        ) : (
          <p className="trailer-modal__unavailable" role="status">
            No verified, embeddable YouTube trailer was found for this title. Try again later or add a verified trailer mapping.
          </p>
        )}
      </section>
    </div>
  )
}