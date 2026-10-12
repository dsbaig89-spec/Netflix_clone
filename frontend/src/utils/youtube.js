export function getYouTubeVideoId(value) {
  if (typeof value !== 'string' || !value.trim()) return ''
  const candidate = value.trim()
  if (/^[\w-]{11}$/.test(candidate)) return candidate

  try {
    const url = new window.URL(candidate)
    if (!['http:', 'https:'].includes(url.protocol)) return ''

    const host = url.hostname.toLowerCase().replace(/^www\./, '')
    let videoId = ''

    if (host === 'youtu.be') {
      videoId = url.pathname.split('/').filter(Boolean)[0] || ''
    } else if (host === 'youtube.com' || host === 'm.youtube.com' || host === 'youtube-nocookie.com') {
      if (url.pathname === '/watch') videoId = url.searchParams.get('v') || ''
      else if (/^\/(embed|shorts|live)\//.test(url.pathname)) videoId = url.pathname.split('/')[2] || ''
    }

    return /^[\w-]{11}$/.test(videoId) ? videoId : ''
  } catch {
    return ''
  }
}