import assert from 'node:assert/strict'
import test from 'node:test'
import { toggleMovieInList } from './myList.js'

test('saves catalogue movies by stable ID without discarding poster or trailer metadata', () => {
  const movie = {
    id: 27,
    title: 'Verified Film',
    posterUrl: 'https://images.example.test/verified-film.jpg',
    trailerUrl: 'https://www.youtube.com/watch?v=abcdefghijk',
    youtubeId: 'abcdefghijk',
  }

  assert.deepEqual(toggleMovieInList([], movie), [movie])
})

test('removes only the selected stable movie ID and leaves other saved movies unchanged', () => {
  const first = { id: 1, title: 'First Film' }
  const second = { id: 2, title: 'Second Film' }

  assert.deepEqual(toggleMovieInList([first, second], { id: 1 }), [second])
})

test('does not duplicate an already saved movie', () => {
  const movie = { id: 1, title: 'First Film' }

  assert.deepEqual(toggleMovieInList([movie], movie), [])
})
