import { useEffect, useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  List,
  ListItem,
  ListItemText,
  Typography,
  Box,
} from '@mui/material'
import { fetchWithAuth } from '../lib/api'

interface HistoryEntry {
  id: number
  assetId: number
  changeType: string
  description: string
  timestamp: string
  changedByUserId?: number | null
}

interface AssetHistoryDialogProps {
  open: boolean
  onClose: () => void
  assetId: number | null
  token: string
}

export function AssetHistoryDialog({ open, onClose, assetId, token }: AssetHistoryDialogProps) {
  const [history, setHistory] = useState<HistoryEntry[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open || !assetId) return
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await fetchWithAuth(`http://localhost:8080/api/inventory/assets/${assetId}/history`, token)
        // Ensure we have an array and sort newest -> oldest by timestamp
        const arr: HistoryEntry[] = Array.isArray(data) ? data : []
        arr.sort((a, b) => {
          const ta = a.timestamp ? new Date(a.timestamp).getTime() : 0
          const tb = b.timestamp ? new Date(b.timestamp).getTime() : 0
          return tb - ta
        })
        setHistory(arr)
      } catch (err: any) {
        setError(err?.message || 'Failed to load history')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [open, assetId, token])

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Asset History</DialogTitle>
      <DialogContent>
        <Box sx={{ mt: 1 }}>
          {loading && <Typography>Loading...</Typography>}
          {error && <Typography color="error">{error}</Typography>}
          {!loading && !error && history.length === 0 && (
            <Typography>No history entries found.</Typography>
          )}
          {!loading && history.length > 0 && (
            <List>
              {history.map((h) => (
                <ListItem key={h.id} alignItems="flex-start">
                  <ListItemText
                    primary={`${h.timestamp ? new Date(h.timestamp).toLocaleString() : ''} — ${h.changeType ?? ''}`}
                    secondary={`${h.description ?? ''} ${h.changedByUserId ? `— By: ${h.changedByUserId}` : ''}`}
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}
