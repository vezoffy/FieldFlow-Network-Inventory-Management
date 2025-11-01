import { useState, useEffect } from 'react';
import { Add as AddIcon } from '@mui/icons-material';
import {
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
  Box,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Paper,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Alert,
  TableContainer
} from '@mui/material';
import { CreateAssetDialog } from './components/CreateAssetDialog';
import { UpdateStatusDialog } from './components/UpdateStatusDialog';
import { AssetHistoryDialog } from './components/AssetHistoryDialog';

interface Asset {
  id: number;
  serialNumber: string;
  assetType: string;
  model: string;
  assetStatus: string;
  location: string;
  assignedToCustomerId: number | null;
  createdAt: string;
}

interface InventoryProps {
  token: string;
  onAuthError?: () => void;
  roles?: string[]; // e.g. ['ROLE_ADMIN', 'ROLE_PLANNER']
}

import { fetchWithAuth, AuthError } from './lib/api';

export function Inventory({ token, onAuthError, roles = [] }: InventoryProps) {
  const [assets, setAssets] = useState<Asset[]>([]);
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isCreateDialogOpen, setCreateDialogOpen] = useState(false);
  const [isStatusDialogOpen, setStatusDialogOpen] = useState(false);
  const [isHistoryDialogOpen, setHistoryDialogOpen] = useState(false);
  const [selectedAssetIdForStatus, setSelectedAssetIdForStatus] = useState<number | null>(null);
  const [selectedAssetIdForHistory, setSelectedAssetIdForHistory] = useState<number | null>(null);

  const fetchAssets = async () => {
    try {
      // Construct the URL with query parameters
      const url = new URL('http://localhost:8080/api/inventory/assets');
      if (typeFilter !== 'all') {
        url.searchParams.append('type', typeFilter);
      }
      if (statusFilter !== 'all') {
        url.searchParams.append('status', statusFilter);
      }
      if (searchTerm !== '') {
        url.searchParams.append('search', searchTerm);
      }

      const data: Asset[] = await fetchWithAuth(url.toString(), token as string)
      setAssets(data)
    } catch (e: any) {
      if (e instanceof AuthError) {
        onAuthError && onAuthError()
        return
      }
      setError('Failed to fetch assets: ' + (e?.message || e))
    }
  };

  useEffect(() => {
    fetchAssets();
  }, [token, typeFilter, statusFilter, searchTerm]);

  const filteredAssets = assets; // Now the API returns the filtered assets

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            Asset Inventory
          </Typography>
          <Typography variant="subtitle1" color="text.secondary">
            Manage network equipment and devices
          </Typography>
        </Box>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => setCreateDialogOpen(true)}
        >
          Add Asset
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Filter Assets
          </Typography>
          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' } }}>
            <TextField
              fullWidth
              placeholder="Search by Serial Number or Location"
              value={searchTerm}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchTerm(e.target.value)}
              variant="outlined"
            />
            <FormControl fullWidth>
              <InputLabel>Asset Type</InputLabel>
              <Select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
                label="Asset Type"
              >
                <MenuItem value="all">All Types</MenuItem>
                <MenuItem value="ONT">ONT</MenuItem>
                <MenuItem value="ROUTER">Router</MenuItem>
                <MenuItem value="SPLITTER">Splitter</MenuItem>
                <MenuItem value="FDH">FDH</MenuItem>
                <MenuItem value="CORE_SWITCH">Core Switch</MenuItem>
                <MenuItem value="HEADEND">Headend</MenuItem>
                <MenuItem value="FIBER_ROLL">Fiber Roll</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                label="Status"
              >
                <MenuItem value="all">All Statuses</MenuItem>
                <MenuItem value="AVAILABLE">Available</MenuItem>
                <MenuItem value="FAULTY">Faulty</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </CardContent>
      </Card>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Serial Number</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Model</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Location</TableCell>
              <TableCell>Created At</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredAssets.map(asset => (
              <TableRow key={asset.id}>
                <TableCell>{asset.serialNumber}</TableCell>
                <TableCell>{asset.assetType}</TableCell>
                <TableCell>{asset.model}</TableCell>
                <TableCell>{asset.assetStatus}</TableCell>
                <TableCell>{asset.location}</TableCell>
                <TableCell>{new Date(asset.createdAt).toLocaleDateString()}</TableCell>
                <TableCell>
                  {/* Show Update Status to ADMIN or TECHNICIAN */}
                  {(roles.includes('ROLE_ADMIN') || roles.includes('ROLE_TECHNICIAN')) && (
                    <Button
                      size="small"
                      variant="outlined"
                      sx={{ mr: 1 }}
                      onClick={() => {
                        setSelectedAssetIdForStatus(asset.id)
                        setStatusDialogOpen(true)
                      }}
                    >
                      Update Status
                    </Button>
                  )}

                  {/* Show History to ADMIN or PLANNER */}
                  {(roles.includes('ROLE_ADMIN') || roles.includes('ROLE_PLANNER')) && (
                    <Button
                      size="small"
                      variant="text"
                      onClick={() => {
                        setSelectedAssetIdForHistory(asset.id)
                        setHistoryDialogOpen(true)
                      }}
                    >
                      View History
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Status update dialog */}
      <UpdateStatusDialog
        open={isStatusDialogOpen}
        onClose={() => { setStatusDialogOpen(false); setSelectedAssetIdForStatus(null); }}
        assetId={selectedAssetIdForStatus}
        token={token}
        onUpdated={fetchAssets}
      />

      {/* Asset history dialog */}
      <AssetHistoryDialog
        open={isHistoryDialogOpen}
        onClose={() => { setHistoryDialogOpen(false); setSelectedAssetIdForHistory(null); }}
        assetId={selectedAssetIdForHistory}
        token={token}
      />

      <CreateAssetDialog
        open={isCreateDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onAssetCreated={fetchAssets}
        token={token}
      />
    </Box>
  );
}