export class AuthError extends Error {
  status: number
  constructor(message: string, status = 401) {
    super(message)
    this.name = 'AuthError'
    this.status = status
  }
}

export async function loginRequest(username: string, password: string) {
  // Debug: log request
  console.debug('loginRequest -> POST /api/auth/login', { username })
  const res = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, password }),
  })

  const text = await res.text().catch(() => '')
  let data: any = {}
  try {
    data = text ? JSON.parse(text) : {}
  } catch (e) {
    // not json
  }

  if (!res.ok) {
    const msg = data?.message || data?.error || text || `Login failed: ${res.status}`
    console.error('loginRequest failed', { status: res.status, body: text })
    throw new Error(msg)
  }

  console.debug('loginRequest success', { status: res.status, body: data })
  return data
}

export async function logoutRequest(token: string | null) {
  if (!token) return
  try {
    await fetch('http://localhost:8080/api/auth/logout', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
  } catch (e) {
    // ignore
  }
}

export async function fetchWithAuth(input: RequestInfo | URL, token: string | null, init?: RequestInit) {
  const headers: Record<string, string> = {
    ...(init && init.headers ? (init.headers as Record<string, string>) : {}),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`
  // Debug: log outgoing request
  console.debug('fetchWithAuth ->', String(input), { init, tokenPresent: !!token })
  const res = await fetch(input, { ...(init || {}), headers })

  const text = await res.text().catch(() => '')
  const contentType = res.headers.get('content-type') || ''

  if (res.status === 401 || res.status === 403) {
    console.warn('fetchWithAuth unauthorized', { url: String(input), status: res.status, body: text })
    throw new AuthError(text || 'Unauthorized', res.status)
  }

  if (!res.ok) {
    console.error('fetchWithAuth http error', { url: String(input), status: res.status, body: text })
    throw new Error(text || `HTTP error ${res.status}`)
  }

  // try parse json, else return text
  if (contentType.includes('application/json')) {
    try {
      return JSON.parse(text)
    } catch (e) {
      return {}
    }
  }
  return text
}
