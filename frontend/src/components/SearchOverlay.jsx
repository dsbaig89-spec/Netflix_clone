import PosterImage from './PosterImage.jsx'

export default function SearchOverlay({ open, query, onQueryChange, onClose, results, loading, error, fallbackMode, notice }) {
  if (!open) {
    return null
  }

  return (
    <div className="search-overlay" aria-modal="true" role="dialog" aria-label="Search movies">
      <div className="search-overlay__panel">
        <div className="search-overlay__header">
          <div className="search-overlay__input-wrap">
            <span className="search-overlay__icon" aria-hidden="true">⌕</span>
            <input
              type="search"
              autoFocus
              value={query}
              onChange={(event) => onQueryChange(event.target.value)}
              placeholder="Search titles, genres, or mood"
              aria-label="Search movies"
            />
            {query ? (
              <button type="button" className="search-overlay__clear" onClick={() => onQueryChange('')} aria-label="Clear search">
                ✕
              </button>
            ) : null}
          </div>
          <button type="button" className="search-overlay__close" onClick={onClose} aria-label="Close search">
            Close
          </button>
        </div>

        {fallbackMode ? <p className="search-overlay__notice">Development fallback is in use because the backend is temporarily unavailable.</p> : null}
        {notice ? <p className="search-overlay__notice" role="status">{notice}</p> : null}

        {loading ? <p className="search-overlay__status">Searching…</p> : null}
        {!loading && error ? <p className="search-overlay__status search-overlay__status--error">{error}</p> : null}

        {!loading && !error && !query ? (
          <div className="search-overlay__empty">
            <p>Search for a title, genre, or mood.</p>
            <small>Try: “light-hearted Indian comedy” or “space exploration sci-fi.”</small>
          </div>
        ) : null}

        {!loading && query && !error && results.length === 0 ? (
          <div className="search-overlay__empty">
            <p>No matches found.</p>
            <small>Try another title or a broader description.</small>
          </div>
        ) : null}

        {results.length > 0 ? (
          <div className="search-overlay__results" aria-live="polite">
            {results.map((movie) => (
              <button key={movie.id} type="button" className="search-result" onClick={() => onClose(movie)}>
                <PosterImage movie={movie} alt={movie.title} loading="lazy" />
                <div className="search-result__details">
                  <strong>{movie.title}</strong>
                  <span>{movie.releaseYear || 'N/A'} • {movie.genres?.slice(0, 2).join(', ') || 'Movie'}</span>
                </div>
              </button>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  )
}
