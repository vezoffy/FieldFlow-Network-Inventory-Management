import { useState } from 'react'
import { Button, Card, CardContent, TextField, Typography, Box, Container } from '@mui/material'
import { loginRequest } from './lib/api'

interface LoginProps {
  onLoginSuccess: (token: string, roles: string[]) => void;
  onLoginError: (message: string) => void;
}

export function Login({ onLoginSuccess, onLoginError }: LoginProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true)
    setErrorMsg(null)
    try {
      const data = await loginRequest(username, password)
      if (data && data.token) {
        onLoginSuccess(data.token, data.roles || [])
      } else {
        setErrorMsg('Invalid response from server')
        onLoginError('Invalid response from server')
      }
    } catch (err: any) {
      const message = err?.message || 'Login failed'
      setErrorMsg(message)
      onLoginError(message)
    } finally {
      setLoading(false)
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(to bottom right, #E6FFFA, #EBF8FF)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2
      }}
    >
      <Container maxWidth="sm">
        <Card sx={{ p: 3 }}>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Box
              sx={{
                width: 64,
                height: 64,
                bgcolor: 'primary.main',
                borderRadius: 2,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto',
                mb: 2
              }}
            >
              <Typography variant="h4" sx={{ color: 'white' }}>
                F
              </Typography>
            </Box>
            <Typography variant="h5" component="h1" gutterBottom>
              FTTH Network Management
            </Typography>
            <Typography variant="subtitle1" color="text.secondary">
              Inventory & Deployment System
            </Typography>
          </Box>
          <CardContent>
            <form onSubmit={handleSubmit}>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <TextField
                  fullWidth
                  id="username"
                  label="Username"
                  variant="outlined"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
                <TextField
                  fullWidth
                  id="password"
                  label="Password"
                  type="password"
                  variant="outlined"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                {errorMsg && (
                  <Typography color="error" variant="body2">
                    {errorMsg}
                  </Typography>
                )}
                <Button
                  type="submit"
                  variant="contained"
                  fullWidth
                  disabled={loading}
                  sx={{ mt: 2 }}
                >
                  {loading ? 'Logging in...' : 'Log In'}
                </Button>
              </Box>
            </form>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}