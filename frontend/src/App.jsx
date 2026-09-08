import './App.css'

function App() {
  return (
    <main className="landing-page">
      <nav className="topbar" aria-label="Primary navigation">
        <a className="wordmark" href="/" aria-label="StreamAI home">STREAM<span>AI</span></a>
        <a className="status-link" href="http://localhost:8080/api/health" target="_blank" rel="noreferrer">
          Backend status <span aria-hidden="true">↗</span>
        </a>
      </nav>

      <section className="hero-content">
        <p className="eyebrow">A new way to find your next story</p>
        <h1>Stories that stay<br /><em>with you.</em></h1>
        <p className="description">
          StreamAI brings thoughtful discovery to movie night, pairing a calm streaming experience with recommendations that become more personal over time.
        </p>
        <a className="primary-button" href="#explore">Explore Movies <span aria-hidden="true">→</span></a>
      </section>

      <section className="signal-strip" id="explore" aria-label="StreamAI features">
        <span>Curated discovery</span>
        <span>Personal taste, amplified</span>
        <span>Built for movie people</span>
      </section>
    </main>
  )
}

export default App
