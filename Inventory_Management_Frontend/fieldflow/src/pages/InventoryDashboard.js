import React, { useEffect, useState, useContext } from 'react';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import InputLabel from '@mui/material/InputLabel';
import FormControl from '@mui/material/FormControl';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import CircularProgress from '@mui/material/CircularProgress';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';

import inventoryService from '../services/inventoryService';
import { AuthContext } from '../context/AuthContext';
import Header from '../components/Header';
import DnsIcon from '@mui/icons-material/Dns';
import SettingsIcon from '@mui/icons-material/Settings';
import CallSplitIcon from '@mui/icons-material/CallSplit';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import StorageIcon from '@mui/icons-material/Storage';
import DevicesIcon from '@mui/icons-material/Devices';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';
import InfoIcon from '@mui/icons-material/Info';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import BugReportIcon from '@mui/icons-material/BugReport';

const assetTypes = ['ONT', 'ROUTER', 'FIBER_ROLL', 'SPLITTER', 'FDH', 'CORE_SWITCH', 'HEADEND'];
const statuses = ['AVAILABLE', 'MAINTENANCE', 'ASSIGNED', 'FAULTY', 'RETIRED'];

const assetIconMap = {
  ONT: DnsIcon,
  ROUTER: SettingsIcon,
  FIBER_ROLL: CallSplitIcon,
  SPLITTER: CallSplitIcon,
  FDH: AccountTreeIcon,
  CORE_SWITCH: StorageIcon,
  HEADEND: DevicesIcon,
};

const statusColorMap = {
  AVAILABLE: ['success', 'main'],
  MAINTENANCE: ['warning', 'main'],
  ASSIGNED: ['info', 'main'],
  FAULTY: ['error', 'main'],
  RETIRED: ['text', 'disabled'],
};

const InventoryDashboard = () => {
  const { user } = useContext(AuthContext);
  const [filters, setFilters] = useState({ type: '', status: '', location: '' });
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(false);

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState({ assetType: 'ONT', name: '', serialNumber: '', model: '', location: '', fdhId: '', portCapacity: '' });

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailAsset, setDetailAsset] = useState(null);
  const [detailStatus, setDetailStatus] = useState('');
  const [splitterDrops, setSplitterDrops] = useState([]);
  const [history, setHistory] = useState([]);

  const [snack, setSnack] = useState({ open: false, message: '', severity: 'success' });
  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [editAssetId, setEditAssetId] = useState(null);
  const [editAssetType, setEditAssetType] = useState(null);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const [assetToDelete, setAssetToDelete] = useState(null);

  const roles = user?.roles || [];
  const canView = roles.some(r => ['ROLE_PLANNER','ROLE_ADMIN','ROLE_SUPPORT_AGENT','ROLE_TECHNICIAN'].includes(r));
  const canCreate = roles.some(r => ['ROLE_PLANNER','ROLE_ADMIN'].includes(r));
  const canChangeStatus = roles.some(r => ['ROLE_TECHNICIAN','ROLE_ADMIN'].includes(r));

  useEffect(() => {
    if (canView) fetchAssets();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchAssets = async () => {
    setLoading(true);
    try {
      // If filtering for fiber rolls (fiber drop lines), call that customers endpoint
      if (filters.type === 'FIBER_ROLL') {
        const fd = await inventoryService.getFiberDropLines();
        // store under assets but keep a marker so rendering knows it's fiber drops
        setAssets(Array.isArray(fd) ? fd.map(d => ({ ...d, __assetCategory: 'FIBER_DROP' })) : []);
      } else {
        const data = await inventoryService.getAssets(filters);
        setAssets(Array.isArray(data) ? data : []);
      }
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Failed to load assets', severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (k, v) => setFilters(prev => ({ ...prev, [k]: v }));

  const openCreate = () => setCreateOpen(true);
  const closeCreate = () => setCreateOpen(false);

  const submitCreate = async () => {
    // validate required fields (name required)
    if (!form.name || !form.name.trim()) {
      setSnack({ open: true, message: 'Name is required', severity: 'error' });
      return;
    }
    try {
      await inventoryService.createAsset(form);
      setSnack({ open: true, message: 'Asset created', severity: 'success' });
      closeCreate();
      fetchAssets();
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Create failed', severity: 'error' });
    }
  };

  const openDetails = async (serial, assetObj) => {
    try {
      // either use provided asset object or fetch by serial
      const a = assetObj || await inventoryService.getAsset(serial);

      // reset splitter drops
      setSplitterDrops([]);

      // if assetType indicates a specialized endpoint, fetch its detail
      if (a.assetType === 'HEADEND') {
        const headend = await inventoryService.getHeadend(a.id);
        setDetailAsset(headend);
        setDetailStatus(headend.assetStatus || '');
      } else if (a.assetType === 'CORE_SWITCH') {
        const cs = await inventoryService.getCoreSwitch(a.id);
        setDetailAsset(cs);
        setDetailStatus(cs.assetStatus || '');
      } else if (a.assetType === 'FDH') {
        const f = await inventoryService.getFdh(a.id);
        setDetailAsset(f);
        setDetailStatus(f.assetStatus || '');
      } else if (a.assetType === 'SPLITTER') {
        const s = await inventoryService.getSplitter(a.id);
        setDetailAsset(s);
        setDetailStatus(s.assetStatus || '');
        // fetch fiber drops for this splitter
        try {
          const drops = await inventoryService.getFiberDropLinesBySplitter(s.id);
          setSplitterDrops(Array.isArray(drops) ? drops : []);
        } catch (e) {
          // non-fatal
          setSplitterDrops([]);
        }
      } else {
        // default asset
        setDetailAsset(a);
        setDetailStatus(a.assetStatus || '');
      }

      const h = await inventoryService.getHistory(a.id);
      setHistory(h || []);
      setDetailOpen(true);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Failed to load details', severity: 'error' });
    }
  };

  const closeDetails = () => setDetailOpen(false);

  const changeStatus = async (id, newStatus) => {
    try {
      await inventoryService.updateStatus(id, newStatus);
      setSnack({ open: true, message: 'Status updated', severity: 'success' });
      fetchAssets();
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Update failed', severity: 'error' });
    }
  };

  const saveDetailStatus = async () => {
    if (!detailAsset) return;
    if (detailStatus === detailAsset.assetStatus) {
      setSnack({ open: true, message: 'No change to save', severity: 'info' });
      return;
    }
    try {
      await changeStatus(detailAsset.id, detailStatus);
      // update local detail asset status
      setDetailAsset(prev => prev ? { ...prev, assetStatus: detailStatus } : prev);
      // refresh history
      const h = await inventoryService.getHistory(detailAsset.id);
      setHistory(h || []);
      setSnack({ open: true, message: 'Status updated', severity: 'success' });
    } catch (e) {
      // changeStatus already sets snack on error
    }
  };

  const isAdmin = roles.includes('ROLE_ADMIN');

  const openEdit = async (asset) => {
    try {
      // asset might be passed as full object; ensure we have id
      const a = asset.id ? asset : await inventoryService.getAsset(asset.serialNumber);
      // fetch canonical asset detail (use type-specific endpoints if available)
      let detail = a;
      if (a.assetType === 'HEADEND') detail = await inventoryService.getHeadend(a.id);
      else if (a.assetType === 'CORE_SWITCH') detail = await inventoryService.getCoreSwitch(a.id);
      else if (a.assetType === 'FDH') detail = await inventoryService.getFdh(a.id);
      else if (a.assetType === 'SPLITTER') detail = await inventoryService.getSplitter(a.id);

      setEditAssetId(detail.id || a.id);
      // determine asset type: prefer original asset param, fall back to detail or heuristics
      const detectedType = asset?.assetType || detail.assetType || (detail.fdhId !== undefined ? 'SPLITTER' : (detail.coreSwitchId !== undefined ? 'CORE_SWITCH' : (detail.headendId !== undefined ? 'HEADEND' : (detail.fdhId !== undefined ? 'FDH' : null))));
      setEditAssetType(detectedType);
      setEditForm({
        location: detail.location || '',
        model: detail.model || '',
        name: detail.name || '',
        region: detail.region || '',
        neighborhood: detail.neighborhood || '',
        portCapacity: detail.portCapacity || '',
        usedPorts: detail.usedPorts !== undefined ? detail.usedPorts : undefined,
      });
      setEditOpen(true);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Failed to load asset for edit', severity: 'error' });
    }
  };

  const closeEdit = () => {
    setEditOpen(false);
    setEditForm({});
    setEditAssetId(null);
    setEditAssetType(null);
  };

  const submitEdit = async () => {
    if (!editAssetId) return;
    // build payload only with fields that are non-empty (or explicitly provided)
    const payload = {};
    // Always allow location and model
    ['location','model','region','neighborhood'].forEach(k => {
      if (editForm[k] !== undefined && editForm[k] !== null && editForm[k] !== '') payload[k] = editForm[k];
    });
    // Name is not present for SPLITTERs per backend; include only when type is not SPLITTER
    if (editAssetType !== 'SPLITTER') {
      if (editForm.name !== undefined && editForm.name !== null && editForm.name !== '') payload.name = editForm.name;
    }
    // portCapacity only for splitters
    if (editAssetType === 'SPLITTER') {
      if (editForm.portCapacity !== undefined && editForm.portCapacity !== null && editForm.portCapacity !== '') payload.portCapacity = editForm.portCapacity;
    }
    try {
      const updated = await inventoryService.updateAsset(editAssetId, payload);
      setSnack({ open: true, message: 'Asset updated', severity: 'success' });
      closeEdit();
      // refresh list and details
      fetchAssets();
      setDetailAsset(updated);
    } catch (e) {
      const code = e?.response?.status;
      const message = e?.response?.data?.message || e.message || 'Update failed';
      setSnack({ open: true, message: message, severity: 'error' });
      if (code === 404) {
        // asset not found
      }
    }
  };

  const openConfirmDelete = (asset) => {
    setAssetToDelete(asset);
    setConfirmDeleteOpen(true);
  };

  const closeConfirmDelete = () => {
    setAssetToDelete(null);
    setConfirmDeleteOpen(false);
  };

  const submitDelete = async () => {
    if (!assetToDelete) return;
    try {
      const resp = await inventoryService.deleteAsset(assetToDelete.id);
      if (resp.status === 204) {
        setSnack({ open: true, message: 'Asset deleted', severity: 'success' });
        closeConfirmDelete();
        fetchAssets();
      } else {
        setSnack({ open: true, message: 'Unexpected delete response', severity: 'warning' });
      }
    } catch (e) {
      const code = e?.response?.status;
      const message = e?.response?.data?.message || e.message || 'Delete failed';
      setSnack({ open: true, message, severity: code === 409 ? 'error' : 'error' });
    }
  };

  if (!canView) return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
      <Typography variant="h5">Access denied</Typography>
      <Typography>You do not have permission to view the inventory dashboard.</Typography>
      </Container>
    </>
  );

  return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Inventory Dashboard</Typography>
        {canCreate && <Button variant="contained" onClick={openCreate}>Add Asset</Button>}
      </Box>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">

          {/* --- Adjusted layout similar to your example --- */}
          {/* Use flexBasis to approximate 3.5 columns (≈29%) so the selects are wider */}
          <Grid item xs={12} sm={3} sx={{ flexBasis: { sm: '29%' } }}>
            <FormControl fullWidth>
              <InputLabel id="filter-type">Type</InputLabel>
              <Select
                labelId="filter-type"
                value={filters.type}
                label="Type"
                onChange={(e) => handleFilterChange('type', e.target.value)}
                MenuProps={{ PaperProps: { sx: { minWidth: 250 } } }}
              >
                <MenuItem value="">All</MenuItem>
                {assetTypes.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12} sm={3} sx={{ flexBasis: { sm: '29%' } }}>
            <FormControl fullWidth>
              <InputLabel id="filter-status">Status</InputLabel>
              <Select
                labelId="filter-status"
                value={filters.status}
                label="Status"
                onChange={(e) => handleFilterChange('status', e.target.value)}
                MenuProps={{ PaperProps: { sx: { minWidth: 250 } } }}
              >
                <MenuItem value="">All</MenuItem>
                {statuses.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12} sm={3}>
            <TextField fullWidth label="Location" value={filters.location} onChange={(e) => handleFilterChange('location', e.target.value)} />
          </Grid>

          <Grid item xs={12} sm={3}>
            <Button variant="outlined" onClick={fetchAssets} fullWidth disabled={loading}>
              {loading ? <CircularProgress size={24} /> : 'Filter'}
            </Button>
          </Grid>

        </Grid>
      </Paper>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Serial</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Model</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Location</TableCell>
              <TableCell>Assigned To</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {assets.map(a => (
              <TableRow key={a.id}>
                {/* If this is a fiber-drop result, render its fields differently */}
                {a.__assetCategory === 'FIBER_DROP' ? (
                  <>
                    <TableCell>{a.id}</TableCell>
                    <TableCell>-</TableCell>
                    <TableCell>Fiber Drop</TableCell>
                    <TableCell>-</TableCell>
                    <TableCell>{a.status}</TableCell>
                    <TableCell>-</TableCell>
                    <TableCell>{a.customerId}</TableCell>
                    <TableCell>-</TableCell>
                    <TableCell>
                      {/* no details by serial here; we could open a modal for fiber drop later */}
                      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                        <Tooltip title="Details">
                          <IconButton size="small" onClick={() => setSnack({ open: true, message: 'Fiber drop details not implemented', severity: 'info' })}>
                            <InfoIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </TableCell>
                  </>
                ) : (
                  <>
                    <TableCell>{a.id}</TableCell>
                    <TableCell>{a.serialNumber}</TableCell>
                    <TableCell>
                      {(() => {
                        const Icon = assetIconMap[a.assetType] || DnsIcon;
                        return (
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Icon fontSize="small" />
                            <Typography variant="body2">{a.assetType}</Typography>
                          </Box>
                        );
                      })()}
                    </TableCell>
                    <TableCell>{a.model}</TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <FiberManualRecordIcon sx={{ color: (theme) => {
                          const key = statusColorMap[a.assetStatus] || ['text','primary'];
                          try {
                            return theme.palette[key[0]]?.[key[1]] || theme.palette.text.primary;
                          } catch (e) {
                            return theme.palette.text.primary;
                          }
                        } }} fontSize="small" />
                        <Typography variant="body2">{a.assetStatus}</Typography>
                      </Box>
                    </TableCell>
                    <TableCell>{a.location}</TableCell>
                    <TableCell>{a.assignedToCustomerId || '-'}</TableCell>
                    <TableCell>{a.createdAt ? new Date(a.createdAt).toLocaleString() : '-'}</TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                        <Tooltip title="Details">
                          <IconButton size="small" onClick={() => openDetails(a.serialNumber, a)}>
                            <InfoIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>

                        {canChangeStatus && (
                          <Tooltip title="Mark Faulty">
                            <IconButton size="small" onClick={() => changeStatus(a.id, 'FAULTY')}>
                              <BugReportIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}

                        {isAdmin && (
                          <>
                            <Tooltip title="Edit">
                              <IconButton size="small" onClick={() => openEdit(a)}>
                                <EditIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>

                            <Tooltip title={a.assetStatus === 'ASSIGNED' ? 'Cannot delete: assigned' : (a.usedPorts !== undefined && a.usedPorts > 0 ? 'Cannot delete: used ports > 0' : 'Delete')}>
                              <span>
                                <IconButton
                                  size="small"
                                  color="error"
                                  onClick={() => openConfirmDelete(a)}
                                  disabled={a.assetStatus === 'ASSIGNED' || (a.usedPorts !== undefined && a.usedPorts > 0)}
                                >
                                  <DeleteIcon fontSize="small" />
                                </IconButton>
                              </span>
                            </Tooltip>
                          </>
                        )}
                      </Box>
                    </TableCell>
                  </>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Edit dialog */}
      <Dialog open={editOpen} onClose={closeEdit} fullWidth maxWidth="sm">
        <DialogTitle>Edit Asset</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 1, display: 'grid', gap: 2 }}>
            {/* show fields relevant to the asset - controlled inputs */}
            {/* Name is not present for splitters in backend models */}
            {editAssetType !== 'SPLITTER' && (
              <TextField label="Name" fullWidth value={editForm.name || ''} onChange={(e) => setEditForm(f => ({ ...f, name: e.target.value }))} />
            )}

            <TextField label="Location" fullWidth value={editForm.location || ''} onChange={(e) => setEditForm(f => ({ ...f, location: e.target.value }))} />
            <TextField label="Model" fullWidth value={editForm.model || ''} onChange={(e) => setEditForm(f => ({ ...f, model: e.target.value }))} />
            <TextField label="Region" fullWidth value={editForm.region || ''} onChange={(e) => setEditForm(f => ({ ...f, region: e.target.value }))} />
            <TextField label="Neighborhood" fullWidth value={editForm.neighborhood || ''} onChange={(e) => setEditForm(f => ({ ...f, neighborhood: e.target.value }))} />

            {/* Port capacity only for splitters */}
            {editAssetType === 'SPLITTER' && (
              <TextField label="Port Capacity" type="number" fullWidth value={editForm.portCapacity || ''} onChange={(e) => setEditForm(f => ({ ...f, portCapacity: e.target.value }))} />
            )}

            {/* Show used ports as read-only if present */}
            {editForm.usedPorts !== undefined && (
              <TextField label="Used Ports" type="number" fullWidth value={editForm.usedPorts} InputProps={{ readOnly: true }} />
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeEdit}>Cancel</Button>
          <Button variant="contained" onClick={submitEdit}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* Delete confirmation dialog */}
      <Dialog open={confirmDeleteOpen} onClose={closeConfirmDelete}>
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <Typography>Are you sure you want to permanently delete asset ID {assetToDelete?.id}?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeConfirmDelete}>Cancel</Button>
          <Button color="error" variant="contained" onClick={submitDelete}>Delete</Button>
        </DialogActions>
      </Dialog>

      {/* Create dialog */}
      <Dialog open={createOpen} onClose={closeCreate} fullWidth maxWidth="sm">
        <DialogTitle>Create Asset</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 1, display: 'grid', gap: 2 }}>
            <FormControl fullWidth>
              <InputLabel id="asset-type">Asset Type</InputLabel>
              <Select labelId="asset-type" value={form.assetType} label="Asset Type" onChange={(e) => setForm(f => ({ ...f, assetType: e.target.value }))}>
                {assetTypes.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Name" required fullWidth value={form.name} onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))} />
            <TextField label="Serial Number" fullWidth value={form.serialNumber} onChange={(e) => setForm(f => ({ ...f, serialNumber: e.target.value }))} />
            <TextField label="Model" fullWidth value={form.model} onChange={(e) => setForm(f => ({ ...f, model: e.target.value }))} />
            <TextField label="Location" fullWidth value={form.location} onChange={(e) => setForm(f => ({ ...f, location: e.target.value }))} />
            {form.assetType === 'SPLITTER' && (
              <TextField label="Port Capacity" fullWidth value={form.portCapacity} onChange={(e) => setForm(f => ({ ...f, portCapacity: e.target.value }))} />
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeCreate}>Cancel</Button>
          <Button variant="contained" onClick={submitCreate}>Create</Button>
        </DialogActions>
      </Dialog>

      {/* Details dialog */}
      <Dialog open={detailOpen} onClose={closeDetails} fullWidth maxWidth="md">
        <DialogTitle>Asset Details</DialogTitle>
        <DialogContent>
          {detailAsset && (
            <Box sx={{ display: 'grid', gap: 2 }}>
              {/* Splitters don't have a name column in the backend; only show name when present and not a splitter */}
              {(detailAsset.assetType !== 'SPLITTER' && detailAsset.name) && (
                <Typography><strong>Name:</strong> {detailAsset.name}</Typography>
              )}
              <Typography><strong>Serial:</strong> {detailAsset.serialNumber}</Typography>
              {/* Hide Type for headend, fdh and core-switch details (those endpoints don't provide an assetType field) */}
              {(!['HEADEND', 'FDH', 'CORE_SWITCH'].includes(detailAsset.assetType)) && (detailAsset.assetType || detailAsset.fdhId !== undefined) && (
                <Typography><strong>Type:</strong> {detailAsset.assetType || (detailAsset.fdhId !== undefined ? 'SPLITTER' : '')}</Typography>
              )}

              {/* type-specific fields */}
              {detailAsset.location && <Typography><strong>Location:</strong> {detailAsset.location}</Typography>}
              {detailAsset.region && <Typography><strong>Region:</strong> {detailAsset.region}</Typography>}
              {detailAsset.headendId !== undefined && <Typography><strong>Headend ID:</strong> {detailAsset.headendId}</Typography>}
              {detailAsset.coreSwitchId !== undefined && <Typography><strong>Core Switch ID:</strong> {detailAsset.coreSwitchId}</Typography>}
              {detailAsset.fdhId !== undefined && <Typography><strong>FDH ID:</strong> {detailAsset.fdhId}</Typography>}
              {detailAsset.portCapacity !== undefined && <Typography><strong>Port Capacity:</strong> {detailAsset.portCapacity}</Typography>}
              {detailAsset.usedPorts !== undefined && <Typography><strong>Used Ports:</strong> {detailAsset.usedPorts}</Typography>}
              {detailAsset.neighborhood && <Typography><strong>Neighborhood:</strong> {detailAsset.neighborhood}</Typography>}
              {detailAsset.model && <Typography><strong>Model:</strong> {detailAsset.model}</Typography>}

              <FormControl fullWidth>
                <InputLabel id="detail-status-label">Status</InputLabel>
                <Select
                  labelId="detail-status-label"
                  value={detailStatus}
                  label="Status"
                  onChange={(e) => setDetailStatus(e.target.value)}
                  disabled={!canChangeStatus}
                  MenuProps={{ PaperProps: { sx: { minWidth: 240 } } }}
                >
                  {statuses.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                </Select>
              </FormControl>

              <Typography sx={{ mt: 2 }} variant="h6">History</Typography>
              {history.length === 0 && <Typography>No history records</Typography>}
              {history.map(h => (
                <Paper key={h.id} sx={{ p: 1, mt: 1 }}>
                  <Typography variant="body2"><strong>{h.changeType}</strong> — {h.description}</Typography>
                  <Typography variant="caption">{new Date(h.timestamp).toLocaleString()}</Typography>
                </Paper>
              ))}

              {/* Splitter-specific: show fiber-drop-lines for this splitter */}
              {detailAsset?.fdhId !== undefined && (
                <>
                  <Typography sx={{ mt: 2 }} variant="h6">Fiber Drops from this Splitter</Typography>
                  {splitterDrops.length === 0 && <Typography>No fiber drops for this splitter</Typography>}
                  {splitterDrops.map(d => (
                    <Paper key={`drop-${d.id}`} sx={{ p: 1, mt: 1 }}>
                      <Typography variant="body2">ID: {d.id} — Customer: {d.customerId} — Length: {d.lengthMeters}m — Status: {d.status}</Typography>
                    </Paper>
                  ))}
                </>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDetails}>Close</Button>
          {canChangeStatus && (
            <Button variant="contained" onClick={saveDetailStatus} disabled={!detailAsset || detailStatus === detailAsset.assetStatus}>
              Update Status
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <Snackbar open={snack.open} autoHideDuration={4000} onClose={() => setSnack(s => ({ ...s, open: false }))}>
        <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.message}</Alert>
      </Snackbar>
      </Container>
    </>
  );
};

export default InventoryDashboard;
