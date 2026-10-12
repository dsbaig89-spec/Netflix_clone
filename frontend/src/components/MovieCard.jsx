import PosterImage from './PosterImage.jsx'

export default function MovieCard({ movie, onSelect, onPlay, onToggleMyList, isInMyList, progress = 0 }) {
  const handleCardSelect = () => {
    if (movie) onSelect(movie)
  }

  return (
    <article className="movie-card" aria-label={movie?.title || 'Movie poster'} onClick={handleCardSelect}>
      <div className="movie-card__poster-wrap">
        <PosterImage className="movie-card__poster" movie={movie} alt={movie?.title || 'Movie poster'} loading="lazy" />

        {progress > 0 ? <div className="movie-card__progress"><span style={{ width: `${Math.min(progress, 100)}%` }} /></div> : null}

        <div className="movie-card__overlay">
          <div className="movie-card__actions">
            <button type="button" className="movie-card__action movie-card__action--primary" aria-label={`Play ${movie?.title}`} onClick={(event) => {
              event.stopPropagation()
              onPlay(movie)
            }}>
              Play
            </button>
            <button type="button" className="movie-card__action movie-card__action--secondary" aria-label={`${isInMyList ? 'Remove from' : 'Add to'} My List`} onClick={(event) => {
              event.stopPropagation()
              onToggleMyList(movie)
            }}>
              {isInMyList ? 'Remove' : 'My List'}
            </button>
            <button type="button" className="movie-card__action movie-card__action--ghost" aria-label={`More info about ${movie?.title}`} onClick={(event) => {
              event.stopPropagation()
              onSelect(movie)
            }}>
              More Info
            </button>
          </div>
        </div>
      </div>

      <div className="movie-card__meta">
        <div className="movie-card__title-meta">
          <h3>{movie?.title}</h3>
          <span>{movie?.rating ? `★ ${Number(movie.rating).toFixed(1)}` : 'New'}</span>
        </div>
        <p>{movie?.releaseYear || '2024'}</p>
      </div>
    </article>
  )
}
