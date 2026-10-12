import { useState } from 'react'
import { registerAccount, signIn } from '../services/authService.js'

const REMEMBERED_EMAIL_KEY = 'streamflix-remembered-email'

function Brand() {
  return <a className="auth-brand" href="/" aria-label="StreamFlix home">Stream<span>Flix</span></a>
}

function PasswordInput({ id, label, value, onChange, autoComplete, minLength }) {
  const [visible, setVisible] = useState(false)
  return (
    <div className="auth-field">
      <label htmlFor={id}>{label}</label>
      <div className="auth-password">
        <input
          id={id}
          name={id}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          minLength={minLength}
          required
        />
        <button
          className="auth-password__toggle"
          type="button"
          onClick={() => setVisible((current) => !current)}
          aria-label={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? 'Hide' : 'Show'}
        </button>
      </div>
    </div>
  )
}

function AuthMessage({ error, notice }) {
  if (error) return <p className="auth-message auth-message--error" role="alert">{error}</p>
  if (notice) return <p className="auth-message auth-message--success" role="status">{notice}</p>
  return null
}

const isValidEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())

export function LoginPage({ onRegister, onLogin, notice, initialEmail, bootstrapError }) {
  const [email, setEmail] = useState(() => initialEmail || localStorage.getItem(REMEMBERED_EMAIL_KEY) || '')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(() => Boolean(localStorage.getItem(REMEMBERED_EMAIL_KEY)))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    if (!email.trim() || !password) {
      setError('Enter your email and password.')
      return
    }
    if (!isValidEmail(email)) {
      setError('Enter a valid email address.')
      return
    }
    setLoading(true)
    try {
      const user = await signIn({ email, password, rememberMe })
      if (rememberMe) localStorage.setItem(REMEMBERED_EMAIL_KEY, email.trim())
      else localStorage.removeItem(REMEMBERED_EMAIL_KEY)
      await onLogin(user)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-screen">
      <div className="auth-backdrop" aria-hidden="true" />
      <header className="auth-header"><Brand /></header>
      <section className="auth-panel" aria-labelledby="login-title">
        <h1 id="login-title">Welcome back</h1>
        <p className="auth-subtitle">Sign in to discover your next favorite.</p>
        <AuthMessage error={error || bootstrapError} notice={notice} />
        <form onSubmit={submit} noValidate>
          <div className="auth-field">
            <label htmlFor="login-email">Email</label>
            <input
              id="login-email"
              name="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              required
            />
          </div>
          <PasswordInput
            id="login-password"
            label="Password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
          />
          <div className="auth-options">
            <label className="auth-checkbox">
              <input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} />
              <span>Remember me</span>
            </label>
            <a href="mailto:support@streamflix.example?subject=Password%20help">Forgot password?</a>
          </div>
          <button className="auth-submit" type="submit" disabled={loading}>
            {loading ? <><span className="auth-spinner" aria-hidden="true" /> Signing in…</> : 'Sign In'}
          </button>
        </form>
        <p className="auth-switch">New to StreamFlix? <button type="button" onClick={onRegister}>Sign up now</button></p>
        <p className="auth-legal">Your films, series, and personalized picks are waiting.</p>
      </section>
    </main>
  )
}

export function RegisterPage({ onLogin, onRegistered, bootstrapError }) {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [fieldError, setFieldError] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setFieldError('')
    if (!fullName.trim() || !email.trim() || !password || !confirmPassword) {
      setError('Complete all fields to create your account.')
      return
    }
    if (!isValidEmail(email)) {
      setError('Enter a valid email address.')
      return
    }
    if (password !== confirmPassword) {
      setFieldError('Passwords do not match.')
      return
    }
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,72}$/.test(password)) {
      setFieldError('Use 8–72 characters with uppercase and lowercase letters, a number, and a symbol.')
      return
    }

    setLoading(true)
    try {
      await registerAccount({ fullName, email, password, confirmPassword })
      onRegistered(email.trim())
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-screen auth-screen--register">
      <div className="auth-backdrop" aria-hidden="true" />
      <header className="auth-header"><Brand /></header>
      <section className="auth-panel auth-panel--register" aria-labelledby="register-title">
        <h1 id="register-title">Create your account</h1>
        <p className="auth-subtitle">Join StreamFlix and find something great to watch.</p>
        <AuthMessage error={error || fieldError || bootstrapError} />
        <form onSubmit={submit} noValidate>
          <div className="auth-field">
            <label htmlFor="register-name">Full name</label>
            <input
              id="register-name"
              name="fullName"
              type="text"
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              autoComplete="name"
              maxLength={120}
              required
            />
          </div>
          <div className="auth-field">
            <label htmlFor="register-email">Email address</label>
            <input
              id="register-email"
              name="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              maxLength={254}
              required
            />
          </div>
          <PasswordInput
            id="register-password"
            label="Password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="new-password"
            minLength={8}
          />
          <PasswordInput
            id="confirm-password"
            label="Confirm password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            autoComplete="new-password"
          />
          <p className="auth-password-hint">Use 8–72 characters with uppercase and lowercase letters, a number, and a symbol.</p>
          <button className="auth-submit" type="submit" disabled={loading}>
            {loading ? <><span className="auth-spinner" aria-hidden="true" /> Creating account…</> : 'Create Account'}
          </button>
        </form>
        <p className="auth-switch">Already have an account? <button type="button" onClick={onLogin}>Sign in</button></p>
      </section>
    </main>
  )
}

export function AuthLoading() {
  return <main className="auth-loading" role="status"><span className="auth-spinner" /> Checking your session…</main>
}
