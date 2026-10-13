import assert from 'node:assert/strict'
import test from 'node:test'
import { getPosterSources, POSTER_PLACEHOLDER } from './posters.js'

test('uses the API-resolved poster before trying an ID-specific local poster', () => {
  assert.deepEqual(getPosterSources({
    id: 21,
    posterUrl: 'https://images.example.test/inception.jpg',
  }), [
    'https://images.example.test/inception.jpg',
    '/images/posters/21.jpg',
    POSTER_PLACEHOLDER,
  ])
})

test('uses the existing poster then placeholder when no movie ID exists', () => {
  assert.deepEqual(getPosterSources({
    posterUrl: 'https://example.test/verified-poster.jpg',
  }), [
    'https://example.test/verified-poster.jpg',
    POSTER_PLACEHOLDER,
  ])
})

test('uses the placeholder only when no poster source is available', () => {
  assert.deepEqual(getPosterSources({ id: null, posterUrl: null }), [POSTER_PLACEHOLDER])
})
