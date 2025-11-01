import { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
  Typography,
} from '@mui/material'
import { fetchWithAuth } from '../lib/api'

interface UpdateStatusDialogProps {
  open: boolean
  onClose: () => void
  assetId: number | null
  token: string
  onUpdated: () => void
}

const STATUS_OPTIONS = [
  'AVAILABLE',
  'FAULTY',
  'ASSIGNED',
  'MAINTENANCE',
  'RETIRED'
]

export function UpdateStatusDialog({ open, onClose, assetId, token, onUpdated }: UpdateStatusDialogProps) {
  const [newStatus, setNewStatus] = useState<string>('AVAILABLE')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault()
    if (!assetId) return
    setError(null)
    setLoading(true)
    try {
      await fetchWithAuth(`http://localhost:8080/api/inventory/assets/${assetId}/status`, token, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newStatus }),
      })

      onUpdated()
      onClose()
    } catch (err: any) {
      setError(err?.message || 'Failed to update status')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Update Asset Status</DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select value={newStatus} label="Status" onChange={(e) => setNewStatus(e.target.value)}>
                {STATUS_OPTIONS.map(s => (
                  <MenuItem key={s} value={s}>{s.replaceAll('_', ' ')}</MenuItem>
                ))}
              </Select>
            </FormControl>
            {error && (
              <Typography color="error" variant="body2">{error}</Typography>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={loading}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}>{loading ? 'Updating...' : 'Update'}</Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
