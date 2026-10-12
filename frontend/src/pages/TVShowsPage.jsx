import MovieRow from '../components/MovieRow.jsx'

export default function TVShowsPage({ movies, myList, onSelect, onPlay, onToggleMyList, progressMap }) {
  const tvShows = movies.filter((movie) => movie.genres.some((genre) => ['Drama', 'Mystery', 'Thriller', 'Comedy'].includes(genre))).slice(0, 12)

  return (
    <div className="page-shell">
      <div className="page-shell__header">
        <h1>TV Shows</h1>
      </div>
      <MovieRow title="New Episodes" movies={tvShows} myList={myList} progressMap={progressMap} onSelect={onSelect} onPlay={onPlay} onToggleMyList={onToggleMyList} />
    </div>
  )
}
