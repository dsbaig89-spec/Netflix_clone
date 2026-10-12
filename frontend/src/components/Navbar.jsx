const navItems = [
  { id: 'home', label: 'Home' },
  { id: 'tv', label: 'TV Shows' },
  { id: 'movies', label: 'Movies' },
  { id: 'popular', label: 'New & Popular' },
  { id: 'my-list', label: 'My List' },
]

function SearchIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="11" cy="11" r="5.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M16 16L21 21" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
    </svg>
  )
}

function BellIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5m6 0a3 3 0 0 1-6 0" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
    </svg>
  )
}

function ChevronIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M7 9.5L12 14.5L17 9.5" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
    </svg>
  )
}

export default function Navbar({ currentView, onNavigate, onOpenSearch, onToggleProfile, user }) {
  const initials = (user?.fullName || 'StreamFlix')
    .split(/\s+/)
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  return (
    <header className="topbar">
      <div className="topbar__inner">
        <div className="topbar__left">
          <button type="button" className="brand" onClick={() => onNavigate('home')} aria-label="Go to home page">
            Stream<span>Flix</span>
          </button>

          <nav className="nav" aria-label="Main navigation">
            {navItems.map((item) => (
              <button
                key={item.id}
                type="button"
                className={currentView === item.id ? 'nav__item nav__item--active' : 'nav__item'}
                onClick={() => onNavigate(item.id)}
              >
                {item.label}
              </button>
            ))}
          </nav>
        </div>

        <div className="topbar__right">
          <button type="button" className="icon-button" aria-label="Open search" onClick={onOpenSearch}>
            <SearchIcon />
          </button>
          <button type="button" className="icon-button" aria-label="Notifications">
            <BellIcon />
          </button>

          <div className="profile-menu-trigger">
            <button type="button" className="profile-button" aria-label="Open profile menu" onClick={onToggleProfile}>
              <span className="profile-avatar">{initials}</span>
              <ChevronIcon />
            </button>
          </div>
        </div>
      </div>
    </header>
  )
}
