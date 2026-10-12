import { useMemo, useState } from 'react'
import MovieCard from '../components/MovieCard.jsx'

const GENRES = ['All', 'Action', 'Adventure', 'Comedy', 'Drama', 'Romance', 'Thriller', 'Horror', 'Science Fiction', 'Fantasy', 'Animation', 'Mystery', 'Crime', 'Documentary', 'Family', 'Indian Cinema']

const normalizeGenre = (value) => {
  const normalized = String(value || '').trim().toLowerCase().replace(/[-_]/g, ' ').replace(/\s+/g, ' ')
  return normalized === 'sci fi' || normalized === 'scifi' ? 'science fiction' : normalized
}

const movieGenres = (movie) => {
  const rawGenres = movie?.genres ?? movie?.genre
  const values = Array.isArray(rawGenres) ? rawGenres : rawGenres ? [rawGenres] : []
  const genres = values.map((genre) => typeof genre === 'string' ? genre : genre?.name).filter(Boolean)
  if (['india', 'indian'].includes(String(movie?.country || '').trim().toLowerCase()) || ['hindi', 'tamil', 'telugu', 'kannada', 'malayalam', 'bengali', 'marathi'].includes(String(movie?.language || '').trim().toLowerCase())) {
    genres.push('Indian Cinema')
  }
  return genres.map(normalizeGenre)
}

export default function MoviesPage({ movies, myList, onSelect, onPlay, onToggleMyList, progressMap }) {
  const [selectedGenre, setSelectedGenre] = useState('All')
  const [query, setQuery] = useState('')

  const movieList = useMemo(() => {
    const normalizedGenre = normalizeGenre(selectedGenre)
    const normalizedQuery = query.trim().toLowerCase()
    return movies.filter((movie) => {
      const hasGenre = selectedGenre === 'All' || movieGenres(movie).includes(normalizedGenre)
      const searchText = [movie.title, movie.overview, ...movieGenres(movie)].join(' ').toLowerCase()
      return hasGenre && (!normalizedQuery || searchText.includes(normalizedQuery))
    })
  }, [movies, query, selectedGenre])

  return (
    <div className="page-shell">
      <div className="page-shell__header">
        <h1>Movies</h1>
        <p className="movie-browser__count">Showing {movieList.length} of {movies.length} loaded movies</p>
      </div>
      <div className="movie-browser__controls">
        <label className="movie-browser__search">
          <span aria-hidden="true">⌕</span>
          <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search titles and synopses" aria-label="Search movies" />
        </label>
        <div className="genre-filters" aria-label="Filter movies by genre">
          {GENRES.map((genre) => (
            <button key={genre} type="button" className={`genre-filter${selectedGenre === genre ? ' genre-filter--active' : ''}`} aria-pressed={selectedGenre === genre} onClick={() => setSelectedGenre(genre)}>
              {genre}
            </button>
          ))}
        </div>
      </div>
      {movieList.length ? (
        <div className="movie-browser__grid">
          {movieList.map((movie) => (
            <MovieCard key={movie.id} movie={movie} onSelect={onSelect} onPlay={onPlay} onToggleMyList={onToggleMyList} isInMyList={myList.some((item) => item.id === movie.id)} progress={progressMap[movie.id] || 0} />
          ))}
        </div>
      ) : (
        <div className="empty-state movie-browser__empty" role="status">
          <div><h2>No movies found</h2><p>Try another genre or search term.</p></div>
        </div>
      )}
    </div>
  )
}
