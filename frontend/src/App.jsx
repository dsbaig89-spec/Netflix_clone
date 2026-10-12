import { useEffect, useMemo, useState } from 'react'
import './App.css'
import Navbar from './components/Navbar.jsx'
import ProfileMenu from './components/ProfileMenu.jsx'
import SearchOverlay from './components/SearchOverlay.jsx'
import MovieDetailsModal from './components/MovieDetailsModal.jsx'
import TrailerModal from './components/TrailerModal.jsx'
import HomePage from './pages/HomePage.jsx'
import MoviesPage from './pages/MoviesPage.jsx'
import TVShowsPage from './pages/TVShowsPage.jsx'
import MyListPage from './pages/MyListPage.jsx'
import { fetchCatalog, getMovieById, searchMovies } from './services/movieService.js'
import { getYouTubeVideoId } from './utils/youtube.js'

const STORAGE_KEY = 'streamflix-my-list'

function App() {
  const [catalog, setCatalog] = useState({ rows: {}, featuredMovie: null, movies: [], continueWatching: [] })
  const [currentView, setCurrentView] = useState('home')
  const [selectedMovie, setSelectedMovie] = useState(null)
  const [trailerMovie, setTrailerMovie] = useState(null)
  const [profileOpen, setProfileOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [fallbackMode, setFallbackMode] = useState(false)
  const [catalogError, setCatalogError] = useState('')
  const [myList, setMyList] = useState(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
      return Array.isArray(saved) ? saved : []
    } catch {
      return []
    }
  })
  const [progressMap, setProgressMap] = useState(() => {
    try {
      const saved = JSON.parse(localStorage.getItem('streamflix-progress') || '{}')
      return saved && typeof saved === 'object' ? saved : {}
    } catch {
      return {}
    }
  })

  useEffect(() => {
    const controller = new AbortController()
    fetchCatalog({ signal: controller.signal })
      .then((payload) => {
        setCatalog({
          ...payload,
          continueWatching: payload.movies.slice(0, 5).map((movie, index) => ({ ...movie, progress: 20 + (index * 12) })),
        })
        setFallbackMode(Boolean(payload.fallback))
        setCatalogError('')
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setCatalogError(`Unable to load the movie catalogue from ${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}. Check that Spring Boot is running and connected to MySQL. No demo-only catalogue is being shown.`)
          setCatalog({ rows: {}, featuredMovie: null, movies: [], continueWatching: [] })
        }
      })

    return () => controller.abort()
  }, [])

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(myList))
  }, [myList])

  useEffect(() => {
    localStorage.setItem('streamflix-progress', JSON.stringify(progressMap))
  }, [progressMap])

  useEffect(() => {
    if (!searchOpen) {
      setSearchQuery('')
      setSearchResults([])
      setSearchError('')
      return
    }

    if (!searchQuery.trim()) {
      setSearchResults([])
      setSearchError('')
      return
    }

    const controller = new AbortController()
    setSearchLoading(true)
    setSearchError('')

    const timeoutId = setTimeout(async () => {
      try {
        const payload = await searchMovies(searchQuery, { signal: controller.signal })
        setSearchResults(payload.results || [])
        setSearchLoading(false)
        setFallbackMode(Boolean(payload.fallback))
      } catch (error) {
        if (error.name !== 'AbortError') {
          setSearchError(error.message || 'Unable to load search results right now.')
          setSearchResults([])
          setSearchLoading(false)
        }
      }
    }, 350)

    return () => {
      clearTimeout(timeoutId)
      controller.abort()
    }
  }, [searchOpen, searchQuery])

  useEffect(() => {
    const onScroll = () => {
      const topbar = document.querySelector('.topbar')
      if (!topbar) return
      topbar.classList.toggle('topbar--scrolled', window.scrollY > 12)
    }

    onScroll()
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const displayedMovies = useMemo(() => {
    if (!catalog.movies?.length) return []
    if (currentView === 'movies') return catalog.movies
    if (currentView === 'tv') return catalog.movies.filter((movie) => movie.genres.some((genre) => ['Drama', 'Mystery', 'Thriller', 'Comedy'].includes(genre)))
    if (currentView === 'my-list') return myList
    if (currentView === 'popular') return catalog.rows.popular || catalog.movies
    return catalog.movies
  }, [catalog, currentView, myList])

  const openMovieModal = (movie) => {
    setSelectedMovie(movie)
    setProfileOpen(false)
  }

  const toggleMyList = (movie) => {
    setMyList((previous) => {
      const exists = previous.some((item) => item.id === movie.id)
      return exists ? previous.filter((item) => item.id !== movie.id) : [...previous, movie]
    })
  }

  const playMovie = async (movie) => {
    if (!movie) return
    let movieWithTrailer = movie
    if (!movie.detailsLoaded && !getYouTubeVideoId(movie.youtubeId) && !getYouTubeVideoId(movie.trailerUrl) && movie.id) {
      const details = await getMovieById(movie.id)
      if (details) movieWithTrailer = details
    }
    setSelectedMovie(movieWithTrailer)
    setTrailerMovie(movieWithTrailer)

    if (getYouTubeVideoId(movieWithTrailer.youtubeId) || getYouTubeVideoId(movieWithTrailer.trailerUrl)) {
      setProgressMap((current) => ({ ...current, [movieWithTrailer.id]: 42 }))
    }
  }

  const renderPage = () => {
    if (currentView === 'movies') {
      return <MoviesPage movies={displayedMovies} myList={myList} onSelect={openMovieModal} onPlay={playMovie} onToggleMyList={toggleMyList} progressMap={progressMap} />
    }

    if (currentView === 'tv') {
      return <TVShowsPage movies={displayedMovies} myList={myList} onSelect={openMovieModal} onPlay={playMovie} onToggleMyList={toggleMyList} progressMap={progressMap} />
    }

    if (currentView === 'my-list') {
      return <MyListPage myList={myList} onSelect={openMovieModal} onPlay={playMovie} onToggleMyList={toggleMyList} progressMap={progressMap} />
    }

    return (
      <HomePage
        catalog={catalog}
        myList={myList}
        onSelect={openMovieModal}
        onPlay={playMovie}
        onToggleMyList={toggleMyList}
        progressMap={progressMap}
      />
    )
  }

  return (
    <div className="app-shell">
      <Navbar
        currentView={currentView}
        onNavigate={(view) => { setCurrentView(view); setProfileOpen(false) }}
        onOpenSearch={() => setSearchOpen(true)}
        onToggleProfile={() => setProfileOpen((open) => !open)}
        profileOpen={profileOpen}
      />

      <ProfileMenu open={profileOpen} onNavigate={(view) => { setCurrentView(view); setProfileOpen(false) }} />

      <main className="page-content">
        {catalogError ? <div className="catalog-error" role="alert">{catalogError}</div> : null}
        {renderPage()}
      </main>

      <SearchOverlay
        open={searchOpen}
        query={searchQuery}
        onQueryChange={setSearchQuery}
        onClose={(movie) => {
          setSearchOpen(false)
          if (movie) openMovieModal(movie)
        }}
        results={searchResults}
        loading={searchLoading}
        error={searchError}
        fallbackMode={fallbackMode}
      />

      <MovieDetailsModal
        movie={selectedMovie}
        allMovies={catalog.movies}
        open={Boolean(selectedMovie)}
        trailerOpen={Boolean(trailerMovie)}
        onClose={() => { setSelectedMovie(null); setTrailerMovie(null) }}
        onPlay={playMovie}
        onToggleMyList={toggleMyList}
        isInMyList={Boolean(selectedMovie && myList.some((item) => item.id === selectedMovie.id))}
        onSelectSimilar={(movie) => setSelectedMovie(movie)}
      />

      <TrailerModal movie={trailerMovie} open={Boolean(trailerMovie)} onClose={() => setTrailerMovie(null)} />
    </div>
  )
}

export default App
