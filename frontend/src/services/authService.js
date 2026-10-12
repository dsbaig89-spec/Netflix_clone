import { API_BASE_URL } from '../config.js'

const apiBaseUrl = () => import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? '' : API_BASE_URL)
let csrfToken = ''

async function refreshCsrfToken() {
  let response
  try {
    response = await fetch(`${apiBaseUrl()}/api/auth/csrf`, {
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('Unable to connect to StreamFlix. Check that the backend is running.')
    }
    throw error
  }
  if (!response.ok) {
    throw new Error('Unable to connect to StreamFlix. Check that the backend is running.')
  }
  const payload = await response.json()
  csrfToken = payload.token
  return csrfToken
}

async function request(path, options = {}, retryCsrf = true) {
  const method = options.method || 'GET'
  const headers = { Accept: 'application/json', ...(options.headers || {}) }

  if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
    headers['Content-Type'] = 'application/json'
    headers['X-CSRF-TOKEN'] = csrfToken || await refreshCsrfToken()
  }

  let response
  try {
    response = await fetch(`${apiBaseUrl()}${path}`, {
      ...options,
      method,
      headers,
      credentials: 'include',
    })
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('Unable to connect to StreamFlix. Check that the backend is running.')
    }
    throw error
  }

  if (response.status === 403 && retryCsrf && method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
    await refreshCsrfToken()
    return request(path, options, false)
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || `Request failed (HTTP ${response.status}). Please try again.`)
  }

  if (response.status === 204) return null
  return response.json()
}

export async function getCurrentUser() {
  await refreshCsrfToken()
  try {
    return await request('/api/auth/me')
  } catch (error) {
    if (error.message === 'Authentication is required') return null
    throw error
  }
}

export function registerAccount(details) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(details),
  })
}

export function signIn(credentials) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
}

export async function signOut() {
  await request('/api/auth/logout', { method: 'POST' })
  csrfToken = ''
  await refreshCsrfToken()
}
