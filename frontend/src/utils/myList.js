export function toggleMovieInList(movies, movie) {
  if (!movie) return movies
  const alreadySaved = movies.some((item) => item.id === movie.id)
  return alreadySaved
    ? movies.filter((item) => item.id !== movie.id)
    : [...movies, movie]
}
