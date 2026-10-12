import MovieRow from '../components/MovieRow.jsx'

export default function MyListPage({ myList, onSelect, onPlay, onToggleMyList, progressMap }) {
  if (!myList.length) {
    return (
      <div className="page-shell page-shell--empty">
        <div className="page-shell__header">
          <h1>My List</h1>
        </div>
        <div className="empty-state">
          <p>Your saved list is empty.</p>
          <span>Add a few movies from the homepage to keep them here.</span>
        </div>
      </div>
    )
  }

  return (
    <div className="page-shell">
      <div className="page-shell__header">
        <h1>My List</h1>
      </div>
      <MovieRow title="Saved Titles" movies={myList} myList={myList} progressMap={progressMap} onSelect={onSelect} onPlay={onPlay} onToggleMyList={onToggleMyList} />
    </div>
  )
}
