import { useRef } from 'react'
import MovieCard from './MovieCard.jsx'

export default function MovieRow({ title, movies = [], onSelect, onPlay, onToggleMyList, myList, progressMap = {} }) {
  const rowRef = useRef(null)

  const scroll = (direction) => {
    if (!rowRef.current) return
    const scrollAmount = direction === 'left' ? -360 : 360
    rowRef.current.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }

  return (
    <section className="movie-row" aria-label={`${title} row`}>
      <div className="movie-row__header">
        <h2>{title}</h2>
        <div className="movie-row__controls">
          <button type="button" className="movie-row__arrow" aria-label={`Scroll ${title} left`} onClick={() => scroll('left')}>
            ‹
          </button>
          <button type="button" className="movie-row__arrow" aria-label={`Scroll ${title} right`} onClick={() => scroll('right')}>
            ›
          </button>
        </div>
      </div>

      <div className="movie-row__track" ref={rowRef}>
        {movies.map((movie) => (
          <MovieCard
            key={movie.id}
            movie={movie}
            onSelect={onSelect}
            onPlay={onPlay}
            onToggleMyList={onToggleMyList}
            isInMyList={myList.some((item) => item.id === movie.id)}
            progress={progressMap[movie.id] || 0}
          />
        ))}
      </div>
    </section>
  )
}
