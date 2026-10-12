export default function HeroBanner({ movie, onPlay, onMoreInfo }) {
  if (!movie) return null

  return (
    <section className="hero-banner" style={{ backgroundImage: `linear-gradient(90deg, rgba(20,20,20,0.86) 0%, rgba(20,20,20,0.58) 40%, rgba(20,20,20,0.18) 100%), url(${movie.backdropUrl})` }}>
      <div className="hero-banner__content">
        <span className="hero-banner__eyebrow">Featured</span>
        <h1>{movie.title}</h1>
        <div className="hero-banner__meta">
          {movie.genres?.slice(0, 3).map((genre) => (
            <span key={genre}>{genre}</span>
          ))}
          <span>{movie.releaseYear || '2024'}</span>
          <span>{movie.rating ? `★ ${Number(movie.rating).toFixed(1)}` : 'New'}</span>
        </div>
        <p>{movie.overview}</p>
        <div className="hero-banner__actions">
          <button type="button" className="hero-banner__button hero-banner__button--primary" onClick={(event) => {
            event.stopPropagation()
            onPlay(movie)
          }}>
            <span aria-hidden="true">▶</span>
            Play
          </button>
          <button type="button" className="hero-banner__button hero-banner__button--secondary" onClick={(event) => {
            event.stopPropagation()
            onMoreInfo(movie)
          }}>
            <span aria-hidden="true">ⓘ</span>
            More Info
          </button>
        </div>
      </div>
    </section>
  )
}
