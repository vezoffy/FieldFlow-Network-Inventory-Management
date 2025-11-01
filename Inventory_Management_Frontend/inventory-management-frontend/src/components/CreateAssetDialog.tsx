import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Box,
} from '@mui/material';
import { fetchWithAuth } from '../lib/api';

interface CreateAssetDialogProps {
  open: boolean;
  onClose: () => void;
  onAssetCreated: () => void;
  token: string;
}

export function CreateAssetDialog({ open, onClose, onAssetCreated, token }: CreateAssetDialogProps) {
  const [formData, setFormData] = useState({
    serialNumber: '',
    assetType: '',
    model: '',
    assetStatus: 'AVAILABLE',
    location: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await fetchWithAuth('http://localhost:8080/api/inventory/assets', token, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      onAssetCreated();
      onClose();
    } catch (err: any) {
      setError(err?.message || 'Failed to create asset');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name!]: value,
    }));
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Create New Asset</DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              name="serialNumber"
              label="Serial Number"
              required
              value={formData.serialNumber}
              onChange={handleChange}
              fullWidth
            />

            <FormControl fullWidth required>
              <InputLabel>Asset Type</InputLabel>
              <Select
                name="assetType"
                value={formData.assetType}
                label="Asset Type"
                onChange={handleChange}
              >
                <MenuItem value="ONT">ONT</MenuItem>
                <MenuItem value="ROUTER">Router</MenuItem>
                <MenuItem value="SPLITTER">Splitter</MenuItem>
                <MenuItem value="FDH">FDH</MenuItem>
                <MenuItem value="CORE_SWITCH">Core Switch</MenuItem>
                <MenuItem value="HEADEND">Headend</MenuItem>
                <MenuItem value="FIBER_ROLL">Fiber Roll</MenuItem>
              </Select>
            </FormControl>

            <TextField
              name="model"
              label="Model"
              required
              value={formData.model}
              onChange={handleChange}
              fullWidth
            />

            <FormControl fullWidth required>
              <InputLabel>Status</InputLabel>
              <Select
                name="assetStatus"
                value={formData.assetStatus}
                label="Status"
                onChange={handleChange}
              >
                <MenuItem value="AVAILABLE">Available</MenuItem>
                <MenuItem value="FAULTY">Faulty</MenuItem>
                <MenuItem value="IN_USE">In Use</MenuItem>
                <MenuItem value="IN_MAINTENANCE">In Maintenance</MenuItem>
              </Select>
            </FormControl>

            <TextField
              name="location"
              label="Location"
              value={formData.location}
              onChange={handleChange}
              fullWidth
            />

            {error && (
              <Box sx={{ color: 'error.main', mt: 1 }}>
                {error}
              </Box>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button
            type="submit"
            variant="contained"
            disabled={loading}
          >
            {loading ? 'Creating...' : 'Create Asset'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}