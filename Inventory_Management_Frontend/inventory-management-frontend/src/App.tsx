import React, { useEffect, useState } from 'react'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import { CssBaseline, AppBar, Toolbar, Typography, Button, Alert, Box, Container } from '@mui/material'
import { Login } from './Login'
import { Inventory } from './Inventory'

const theme = createTheme({
  palette: {
    primary: {
      main: '#009688', // Teal color
    },
    secondary: {
      main: '#2196f3', // Blue color
    },
    background: {
      default: '#f0f2f5',
    },
  },
});

function parseJwtRoles(token: string): string[] {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return []
    const payload = parts[1]
    // base64url -> base64
    const b64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(b64)
        .split('')
        .map(function (c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        })
        .join(''),
    )
    const obj = JSON.parse(json)
    // common claim name is roles or authorities
    if (Array.isArray(obj.roles)) return obj.roles
    if (Array.isArray(obj.authorities)) return obj.authorities
    // sometimes role strings are under 'role' or 'authorities' as objects
    return []
  } catch (e) {
    return []
  }
}

function App() {
  const [token, setToken] = useState<string | null>(null)
  const [roles, setRoles] = useState<string[]>([])
  const [loginError, setLoginError] = useState<string | null>(null)

  useEffect(() => {
    // Restore token from localStorage if present
    const saved = localStorage.getItem('token')
    if (saved) {
      setToken(saved)
      setRoles(parseJwtRoles(saved))
    }
  }, [])

  const handleLoginSuccess = (token: string, rolesFromServer: string[]) => {
    setToken(token)
    // prefer server-provided roles, fallback to parsed JWT
    const r = Array.isArray(rolesFromServer) && rolesFromServer.length ? rolesFromServer : parseJwtRoles(token)
    setRoles(r)
    setLoginError(null)
    localStorage.setItem('token', token)
  }

  const handleLoginError = (message: string) => {
    setToken(null)
    setRoles([])
    setLoginError(message)
    localStorage.removeItem('token')
  }

  const isAdmin = roles.includes('ROLE_ADMIN')
  const isPlannerOrSupport = roles.includes('ROLE_PLANNER') || roles.includes('ROLE_SUPPORT_AGENT')

  const handleLogout = async () => {
    const t = token
    try {
      if (t) {
        await fetch('http://localhost:8080/api/auth/logout', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${t}`,
          },
        })
      }
    } catch (e) {
      // ignore errors on logout, continue clearing client state
      console.error('Logout request failed', e)
    }

    setToken(null)
    setRoles([])
    localStorage.removeItem('token')
  }

  // callback for children when a 401/invalid token is detected
  const handleAuthError = () => {
    setLoginError('Session expired or unauthorized. Please log in again.')
    handleLogout()
  }

  if (!token) {
    return <Login onLoginSuccess={handleLoginSuccess} onLoginError={handleLoginError} />
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
        {token && (
          <AppBar position="static" color="primary">
            <Toolbar>
              <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                Welcome, {isAdmin ? 'Admin' : isPlannerOrSupport ? 'Planner/Support Agent' : 'User'}!
              </Typography>
              <Button color="inherit" onClick={handleLogout}>
                Logout
              </Button>
            </Toolbar>
          </AppBar>
        )}

        <Container maxWidth={false} sx={{ flex: 1, py: 3 }}>
          {loginError && (
            <Alert severity="error" sx={{ mb: 3 }}>
              <strong>Error!</strong> {loginError}
            </Alert>
          )}

          {token ? (
            <Box>
              <Typography variant="h1" sx={{ position: 'absolute', overflow: 'hidden', width: 1, height: 1, clip: 'rect(0 0 0 0)' }}>
                Dashboard
              </Typography>
              <Inventory token={token} onAuthError={handleAuthError} roles={roles} />
            </Box>
          ) : (
            <Login onLoginSuccess={handleLoginSuccess} onLoginError={handleLoginError} />
          )}
        </Container>
      </Box>
    </ThemeProvider>
  )
}

export default App