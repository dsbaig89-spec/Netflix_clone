import assert from 'node:assert/strict'
import test from 'node:test'
import { getPosterSources, POSTER_PLACEHOLDER } from './posters.js'

test('uses an ID-specific local poster before preserving the existing verified URL', () => {
  assert.deepEqual(getPosterSources({
    id: 21,
    posterUrl: 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg',
  }), [
    '/images/posters/21.jpg',
    'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg',
    POSTER_PLACEHOLDER,
  ])
})

test('uses the existing poster before the placeholder when no movie ID exists', () => {
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
