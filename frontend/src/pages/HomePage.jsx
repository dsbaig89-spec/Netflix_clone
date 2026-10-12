import HeroBanner from '../components/HeroBanner.jsx'
import MovieRow from '../components/MovieRow.jsx'

export default function HomePage({ catalog, myList, onSelect, onPlay, onToggleMyList, progressMap }) {
  const rowConfig = [
    { key: 'trending', title: 'Trending Now' },
    { key: 'popular', title: 'Popular Movies' },
    { key: 'topRated', title: 'Top Rated' },
    { key: 'action', title: 'Action & Adventure' },
    { key: 'indian', title: 'Indian Movies' },
    { key: 'sciFi', title: 'Sci-Fi Movies' },
    { key: 'comedy', title: 'Comedy Movies' },
    { key: 'continueWatching', title: 'Continue Watching' },
  ]

  return (
    <>
      <HeroBanner movie={catalog.featuredMovie} onPlay={onPlay} onMoreInfo={onSelect} />
      {rowConfig.map((row) => {
        const movies = row.key === 'continueWatching' ? catalog.continueWatching || [] : catalog.rows?.[row.key] || []
        if (!movies.length) return null

        return (
          <MovieRow
            key={row.key}
            title={row.title}
            movies={movies}
            myList={myList}
            progressMap={progressMap}
            onSelect={onSelect}
            onPlay={onPlay}
            onToggleMyList={onToggleMyList}
          />
        )
      })}
    </>
  )
}
