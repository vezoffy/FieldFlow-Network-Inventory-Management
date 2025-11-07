import React, { useContext, useEffect, useState } from 'react';
import Header from '../components/Header';
import Autocomplete from '@mui/material/Autocomplete';
import { Container, Typography, Paper, Box, Button, TextField, Grid, Table, TableBody, TableCell, TableHead, TableRow, IconButton, Dialog, DialogTitle, DialogContent, DialogActions, MenuItem, Select, InputLabel, FormControl, Snackbar, Alert } from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import PeopleIcon from '@mui/icons-material/People';
import StorageIcon from '@mui/icons-material/Storage';
import customerService from '../services/customerService';
import api from '../api/axiosInstance';
import { AuthContext } from '../context/AuthContext';

const emptyForm = { name: '', address: '', neighborhood: '', plan: '', connectionType: 'WIRED' };

const CustomerDashboard = () => {
  const { user } = useContext(AuthContext);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchName, setSearchName] = useState('');
  const [searchNeighborhood, setSearchNeighborhood] = useState('');
  const [searchStatus, setSearchStatus] = useState('');
  const [selected, setSelected] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [assignAssetOpen, setAssignAssetOpen] = useState(false);
  const [assignPortOpen, setAssignPortOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });

  const canCreate = user?.roles?.includes('ROLE_PLANNER') || user?.roles?.includes('ROLE_ADMIN');
  const canViewAll = user?.roles?.includes('ROLE_PLANNER') || user?.roles?.includes('ROLE_ADMIN') || user?.roles?.includes('ROLE_SUPPORT_AGENT') || user?.roles?.includes('ROLE_TECHNICIAN');
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');

  useEffect(() => {
    if (canViewAll) loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const loadAll = async () => {
    setLoading(true);
    try {
      const data = await customerService.getAllCustomers();
      setCustomers(data || []);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Failed to load customers', severity: 'error' });
    } finally { setLoading(false); }
  };

  const handleSearch = async () => {
    setLoading(true);
    try {
      const params = {};
  if (searchName) params.name = searchName;
  if (searchNeighborhood) params.neighborhood = searchNeighborhood;
  if (searchStatus) params.status = searchStatus;
      const data = await customerService.searchCustomers(params);
      setCustomers(data || []);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Search failed', severity: 'error' });
    } finally { setLoading(false); }
  };

  const openCreate = () => { setForm(emptyForm); setCreateOpen(true); };
  const submitCreate = async () => {
    try {
      await customerService.createCustomer(form);
      setCreateOpen(false);
      setSnack({ open: true, message: 'Customer created (PENDING).', severity: 'success' });
      loadAll();
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Create failed', severity: 'error' });
    }
  };

  const openEdit = (c) => { setSelected(c); setForm({ name: c.name, address: c.address, neighborhood: c.neighborhood, plan: c.plan, connectionType: c.connectionType }); setEditOpen(true); };
  const submitEdit = async () => {
    try {
      await customerService.updateCustomer(selected.id, form);
      setEditOpen(false);
      setSnack({ open: true, message: 'Customer updated.', severity: 'success' });
      loadAll();
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Update failed', severity: 'error' });
    }
  };

  const confirmDelete = async (c) => {
    if (!isAdmin) return;
    if (c.status !== 'INACTIVE') {
      setSnack({ open: true, message: 'Only INACTIVE customers can be deleted.', severity: 'warning' });
      return;
    }
    try {
      const resp = await customerService.deleteCustomer(c.id);
      if (resp.status === 204) {
        setSnack({ open: true, message: 'Customer deleted.', severity: 'success' });
        setCustomers(prev => prev.filter(x => x.id !== c.id));
      }
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Delete failed', severity: 'error' });
    }
  };

  // Assign asset dialog
  const [assetSerial, setAssetSerial] = useState('');
  const [assetOptions, setAssetOptions] = useState([]);
  const [assetOptionsLoading, setAssetOptionsLoading] = useState(false);
  const openAssignAsset = (c) => { setSelected(c); setAssetSerial(''); setAssignAssetOpen(true); loadAssetOptions(); };
  const submitAssignAsset = async () => {
    try {
      await customerService.assignAsset(selected.id, assetSerial);
      setAssignAssetOpen(false);
      setSnack({ open: true, message: 'Asset assigned.', severity: 'success' });
      loadAll();
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Assign asset failed', severity: 'error' });
    }
  };

  // Assign / Reassign port dialog
  const [portBody, setPortBody] = useState({ splitterSerialNumber: '', portNumber: '', lengthMeters: '' });
  const [splitterOptions, setSplitterOptions] = useState([]);
  const [splitterOptionsLoading, setSplitterOptionsLoading] = useState(false);
  const openAssignPort = (c) => { setSelected(c); setPortBody({ splitterSerialNumber: '', portNumber: '', lengthMeters: '' }); setAssignPortOpen(true); loadSplitterOptions(); };
  const submitAssignPort = async () => {
    try {
      const body = { splitterSerialNumber: portBody.splitterSerialNumber, portNumber: Number(portBody.portNumber), lengthMeters: Number(portBody.lengthMeters) };
      if (selected.status === 'PENDING') {
        await customerService.assignPort(selected.id, body);
        setSnack({ open: true, message: 'Port assigned. Customer is now ACTIVE.', severity: 'success' });
      } else if (selected.status === 'ACTIVE') {
        await customerService.reassignPort(selected.id, body);
        setSnack({ open: true, message: 'Port reassigned.', severity: 'success' });
      }
      setAssignPortOpen(false);
      loadAll();
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Assign port failed', severity: 'error' });
    }
  };

  // Load asset options (ONT/ROUTER available)
  const loadAssetOptions = async () => {
    setAssetOptionsLoading(true);
    try {
      const resp = await api.get('/api/inventory/assets');
      const items = resp.data || [];
      // prefer AVAILABLE ONT/ROUTER
      const opts = items
        .filter(a => ['ONT', 'ROUTER'].includes(a.assetType))
        .map(a => ({ label: a.serialNumber, value: a.serialNumber, id: a.id, model: a.model }));
      setAssetOptions(opts);
    } catch (e) {
      // ignore
    } finally { setAssetOptionsLoading(false); }
  };

  // Load splitter options (splitter serials)
  const loadSplitterOptions = async () => {
    setSplitterOptionsLoading(true);
    try {
      const resp = await api.get('/api/inventory/assets');
      const items = resp.data || [];
      const opts = items
        .filter(a => a.assetType === 'SPLITTER')
        .map(a => ({ label: a.serialNumber, value: a.serialNumber, id: a.id, model: a.model }));
      setSplitterOptions(opts);
    } catch (e) {
      // ignore
    } finally { setSplitterOptionsLoading(false); }
  };

  if (!canViewAll) return (
    <>
      <Header />
      <Container sx={{ pt: 12 }}>
        <Typography variant="h6">You do not have permission to view customers.</Typography>
      </Container>
    </>
  );

  return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
        <Typography variant="h4" sx={{ mb: 2 }}>Customer Management</Typography>

        <Paper sx={{ p: 2, mb: 2 }}>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={3}>
              <TextField label="Name" size="small" fullWidth value={searchName} onChange={(e) => setSearchName(e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={3}>
              <TextField label="Neighborhood" size="small" fullWidth value={searchNeighborhood} onChange={(e) => setSearchNeighborhood(e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Status</InputLabel>
                <Select value={searchStatus} label="Status" onChange={(e) => setSearchStatus(e.target.value)}>
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="PENDING">PENDING</MenuItem>
                  <MenuItem value="ACTIVE">ACTIVE</MenuItem>
                  <MenuItem value="INACTIVE">INACTIVE</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item>
              <Button variant="contained" onClick={handleSearch}>Search</Button>
            </Grid>
            <Grid item sx={{ ml: 'auto' }}>
              {canCreate && <Button startIcon={<AddIcon />} variant="contained" onClick={openCreate}>Create Customer</Button>}
            </Grid>
          </Grid>
        </Paper>

        <Paper sx={{ p: 0 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Address</TableCell>
                <TableCell>Neighborhood</TableCell>
                <TableCell>Plan</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Splitter</TableCell>
                <TableCell>Port</TableCell>
                <TableCell>Assets</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {customers.map(c => (
                <TableRow key={c.id}>
                  <TableCell>{c.id}</TableCell>
                  <TableCell>{c.name}</TableCell>
                  <TableCell>{c.address}</TableCell>
                  <TableCell>{c.neighborhood}</TableCell>
                  <TableCell>{c.plan}</TableCell>
                  <TableCell>{c.status}</TableCell>
                  <TableCell>{c.splitterSerialNumber}</TableCell>
                  <TableCell>{c.assignedPort}</TableCell>
                  <TableCell>{(c.assignedAssets || []).map(a => a.serialNumber).join(', ')}</TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => openEdit(c)}><EditIcon /></IconButton>
                    <IconButton size="small" onClick={() => openAssignAsset(c)}><StorageIcon /></IconButton>
                    <IconButton size="small" onClick={() => openAssignPort(c)}>P</IconButton>
                    {isAdmin && <IconButton size="small" onClick={() => confirmDelete(c)} disabled={c.status !== 'INACTIVE'}><DeleteIcon /></IconButton>}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>

        {/* Create Dialog */}
        <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>Create Customer</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 1, display: 'grid', gap: 2 }}>
              <TextField label="Name" value={form.name} onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))} />
              <TextField label="Address" value={form.address} onChange={(e) => setForm(f => ({ ...f, address: e.target.value }))} />
              <TextField label="Neighborhood" value={form.neighborhood} onChange={(e) => setForm(f => ({ ...f, neighborhood: e.target.value }))} />
              <TextField label="Plan" value={form.plan} onChange={(e) => setForm(f => ({ ...f, plan: e.target.value }))} />
              <FormControl>
                <InputLabel>Connection</InputLabel>
                <Select value={form.connectionType} label="Connection" onChange={(e) => setForm(f => ({ ...f, connectionType: e.target.value }))}>
                  <MenuItem value="WIRED">WIRED</MenuItem>
                  <MenuItem value="WIRELESS">WIRELESS</MenuItem>
                </Select>
              </FormControl>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
            <Button variant="contained" onClick={submitCreate}>Create</Button>
          </DialogActions>
        </Dialog>

        {/* Edit Dialog */}
        <Dialog open={editOpen} onClose={() => setEditOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>Edit Customer</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 1, display: 'grid', gap: 2 }}>
              <TextField label="Name" value={form.name} onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))} />
              <TextField label="Address" value={form.address} onChange={(e) => setForm(f => ({ ...f, address: e.target.value }))} />
              <TextField label="Neighborhood" value={form.neighborhood} onChange={(e) => setForm(f => ({ ...f, neighborhood: e.target.value }))} />
              <TextField label="Plan" value={form.plan} onChange={(e) => setForm(f => ({ ...f, plan: e.target.value }))} />
              <FormControl>
                <InputLabel>Connection</InputLabel>
                <Select value={form.connectionType} label="Connection" onChange={(e) => setForm(f => ({ ...f, connectionType: e.target.value }))}>
                  <MenuItem value="WIRED">WIRED</MenuItem>
                  <MenuItem value="WIRELESS">WIRELESS</MenuItem>
                </Select>
              </FormControl>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setEditOpen(false)}>Cancel</Button>
            <Button variant="contained" onClick={submitEdit}>Save</Button>
          </DialogActions>
        </Dialog>

        {/* Assign Asset Dialog */}
        <Dialog open={assignAssetOpen} onClose={() => setAssignAssetOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>Assign Asset to {selected?.name}</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 1 }}>
              <Autocomplete
                freeSolo
                options={assetOptions}
                loading={assetOptionsLoading}
                getOptionLabel={(opt) => (typeof opt === 'string' ? opt : (opt.label || opt.value || ''))}
                inputValue={assetSerial}
                onInputChange={(e, val) => setAssetSerial(val)}
                onChange={(e, val) => {
                  if (!val) return;
                  if (typeof val === 'string') setAssetSerial(val);
                  else setAssetSerial(val.value ?? val.label ?? '');
                }}
                renderInput={(params) => <TextField {...params} label="Asset Serial Number" fullWidth />}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAssignAssetOpen(false)}>Cancel</Button>
            <Button variant="contained" onClick={submitAssignAsset}>Assign</Button>
          </DialogActions>
        </Dialog>

        {/* Assign Port Dialog */}
        <Dialog open={assignPortOpen} onClose={() => setAssignPortOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>{selected?.status === 'PENDING' ? 'Assign Port (initial)' : 'Reassign Port'}</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 1, display: 'grid', gap: 2 }}>
              <Autocomplete
                freeSolo
                options={splitterOptions}
                loading={splitterOptionsLoading}
                getOptionLabel={(opt) => (typeof opt === 'string' ? opt : (opt.label || opt.value || ''))}
                inputValue={portBody.splitterSerialNumber}
                onInputChange={(e, val) => setPortBody(b => ({ ...b, splitterSerialNumber: val }))}
                onChange={(e, val) => {
                  if (!val) return;
                  if (typeof val === 'string') setPortBody(b => ({ ...b, splitterSerialNumber: val }));
                  else setPortBody(b => ({ ...b, splitterSerialNumber: val.value ?? val.label ?? '' }));
                }}
                renderInput={(params) => <TextField {...params} label="Splitter Serial" />}
              />
              <TextField label="Port Number" value={portBody.portNumber} onChange={(e) => setPortBody(b => ({ ...b, portNumber: e.target.value }))} />
              <TextField label="Length (meters)" value={portBody.lengthMeters} onChange={(e) => setPortBody(b => ({ ...b, lengthMeters: e.target.value }))} />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAssignPortOpen(false)}>Cancel</Button>
            <Button variant="contained" onClick={submitAssignPort}>Submit</Button>
          </DialogActions>
        </Dialog>

        <Snackbar open={snack.open} autoHideDuration={5000} onClose={() => setSnack(s => ({ ...s, open: false }))}>
          <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.message}</Alert>
        </Snackbar>
      </Container>
    </>
  );
};

export default CustomerDashboard;
