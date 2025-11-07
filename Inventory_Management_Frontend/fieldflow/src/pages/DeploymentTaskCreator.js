import React, { useContext, useEffect, useState } from 'react';
import Header from '../components/Header';
import { Container, Typography, Paper, Box, Grid, Button, TextField, Snackbar, Alert } from '@mui/material';
import Autocomplete from '@mui/material/Autocomplete';
import customerService from '../services/customerService';
import deploymentService from '../services/deploymentService';
import api from '../api/axiosInstance';
import { AuthContext } from '../context/AuthContext';

const DeploymentTaskCreator = () => {
  const { user } = useContext(AuthContext);
  // Allow TECHNICIAN and ADMIN to create deployment tasks in the UI per role mapping.
  // NOTE: backend may still restrict certain endpoints to ADMIN; if calls fail with 403 we'll surface the error.
  const canCreate = user?.roles?.includes('ROLE_ADMIN') || user?.roles?.includes('ROLE_TECHNICIAN');

  const [customers, setCustomers] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [custOption, setCustOption] = useState(null);
  const [techOption, setTechOption] = useState(null);
  const [date, setDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });

  useEffect(() => {
    if (!canCreate) return;
    loadPendingCustomers();
    loadTechnicians();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const loadPendingCustomers = async () => {
    try {
      const res = await customerService.searchCustomers({ status: 'PENDING' });
      const opts = (res || []).map(c => ({ label: `${c.id} — ${c.name}`, value: String(c.id), id: c.id, name: c.name }));
      setCustomers(opts);
    } catch (e) {
      setSnack({ open: true, message: 'Failed to load customers', severity: 'error' });
    }
  };

  const loadTechnicians = async () => {
    try {
      // Prefer using service wrapper which may accept query params later
      const list = await deploymentService.getTechnicians().catch(() => []);
      const opts = (list || []).map(t => ({ label: t.name || t.username || `Tech-${t.id}`, value: String(t.id), id: t.id, name: t.name || t.username, contact: t.contact, region: t.region }));
      setTechnicians(opts);
    } catch (e) {
      const msg = e?.response?.status === 403 ? 'Access denied: administrators only' : 'Failed to load technicians';
      setSnack({ open: true, message: msg, severity: 'error' });
    }
  };

  const submit = async () => {
    if (!custOption || !techOption || !date) {
      setSnack({ open: true, message: 'Please select customer, technician and date', severity: 'warning' });
      return;
    }
    setLoading(true);
    try {
      const body = { customerId: Number(custOption.value), technicianId: Number(techOption.value), scheduledDate: date };
      const resp = await deploymentService.createTask(body);
      if (resp.status === 201) {
        setSnack({ open: true, message: 'Deployment task created', severity: 'success' });
        // reset
        setCustOption(null);
        setTechOption(null);
        setDate('');
        // optionally refresh pending customers and technicians
        await loadPendingCustomers();
      } else {
        setSnack({ open: true, message: 'Unexpected response', severity: 'warning' });
      }
    } catch (e) {
      const msg = e?.response?.data?.message || e.message || 'Failed to create task';
      setSnack({ open: true, message: msg, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!canCreate) return (
    <>
      <Header />
      <Container sx={{ pt: 12 }}>
        <Typography variant="h6">You do not have permission to create deployment tasks.</Typography>
      </Container>
    </>
  );

  return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
        <Typography variant="h4" sx={{ mb: 2 }}>Create Deployment Task</Typography>

        <Paper sx={{ p: 3, mb: 3 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Autocomplete
              options={customers}
              getOptionLabel={(o) => o.label || ''}
              value={custOption}
              onChange={(e, v) => setCustOption(v)}
              renderInput={(params) => <TextField {...params} label="Select pending customer" placeholder="Type id or name" />}
              fullWidth
              freeSolo={false}
            />

            <Autocomplete
              options={technicians}
              getOptionLabel={(o) => o.label || ''}
              value={techOption}
              onChange={(e, v) => setTechOption(v)}
              renderInput={(params) => <TextField {...params} label="Select technician" placeholder="Type name" />}
              fullWidth
              freeSolo={false}
            />

            <TextField
              label="Scheduled Date"
              type="date"
              InputLabelProps={{ shrink: true }}
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />

            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button variant="contained" onClick={submit} disabled={loading}>{loading ? 'Creating...' : 'Create Task'}</Button>
              <Button variant="outlined" onClick={() => { setCustOption(null); setTechOption(null); setDate(''); }}>Reset</Button>
            </Box>
          </Box>
        </Paper>

        <Snackbar open={snack.open} autoHideDuration={6000} onClose={() => setSnack(s => ({ ...s, open: false }))}>
          <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.message}</Alert>
        </Snackbar>
      </Container>
    </>
  );
};

export default DeploymentTaskCreator;
