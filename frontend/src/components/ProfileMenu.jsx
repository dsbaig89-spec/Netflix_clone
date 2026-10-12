export default function ProfileMenu({ open, onNavigate, onLogout, fullName }) {
  if (!open) {
    return null
  }

  return (
    <div className="profile-menu" role="menu" aria-label="Profile menu">
      <span className="profile-menu__name">{fullName}</span>
      <button type="button" className="profile-menu__item" onClick={() => onNavigate('home')} role="menuitem">
        Home
      </button>
      <button type="button" className="profile-menu__item" onClick={() => onNavigate('my-list')} role="menuitem">
        My List
      </button>
      <button type="button" className="profile-menu__item" onClick={() => onNavigate('popular')} role="menuitem">
        New &amp; Popular
      </button>
      <button type="button" className="profile-menu__item profile-menu__item--logout" onClick={onLogout} role="menuitem">
        Sign out
      </button>
    </div>
  )
}
