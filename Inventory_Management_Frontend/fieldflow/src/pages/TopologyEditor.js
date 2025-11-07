import React, { useContext, useEffect, useState } from 'react';
import Header from '../components/Header';
import { Container, Typography, Paper, Box, Grid, FormControl, InputLabel, Select, MenuItem, Button, Snackbar, Alert, CircularProgress } from '@mui/material';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import inventoryService from '../services/inventoryService';
import { AuthContext } from '../context/AuthContext';

const deviceTypes = [
  { value: 'CORE_SWITCH', label: 'Core Switch' },
  { value: 'FDH', label: 'FDH' },
  { value: 'SPLITTER', label: 'Splitter' },
];

const TopologyEditor = () => {
  const { user } = useContext(AuthContext);
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');
  const canAccess = isAdmin || user?.roles?.includes('ROLE_PLANNER');

  const [deviceType, setDeviceType] = useState('CORE_SWITCH');

  const [childOptions, setChildOptions] = useState([]);
  const [parentOptions, setParentOptions] = useState([]);
  const [child, setChild] = useState(null);
  const [parent, setParent] = useState(null);

  const [loadingChildren, setLoadingChildren] = useState(false);
  const [loadingParents, setLoadingParents] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });

  useEffect(() => {
    if (!canAccess) return;
    // load children for initial deviceType
    loadChildOptions(deviceType);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deviceType, isAdmin]);

  const loadChildOptions = async (type) => {
    setLoadingChildren(true);
    setChild(null);
    setParent(null);
    setLoadingParents(true);
    try {
      if (type === 'CORE_SWITCH') {
        const cs = await inventoryService.getCoreSwitches();
        setChildOptions((cs || []).map(c => ({ label: c.name || c.serialNumber || `Core-${c.id}`, value: c.id, meta: c })));
        // parent options are headends
        const hs = await inventoryService.getHeadends();
        setParentOptions((hs || []).map(h => ({ label: h.name || h.serialNumber || `Headend-${h.id}`, value: h.id, meta: h })));
      } else if (type === 'FDH') {
        const f = await inventoryService.getFdhs();
        setChildOptions((f || []).map(x => ({ label: x.name || x.serialNumber || `FDH-${x.id}`, value: x.id, meta: x })));
        const cs = await inventoryService.getCoreSwitches();
        setParentOptions((cs || []).map(c => ({ label: c.name || c.serialNumber || `Core-${c.id}`, value: c.id, meta: c })));
      } else if (type === 'SPLITTER') {
        const s = await inventoryService.getSplitters();
        setChildOptions((s || []).map(x => ({ label: x.serialNumber || `Splitter-${x.id}`, value: x.id, meta: x })));
        const f = await inventoryService.getFdhs();
        setParentOptions((f || []).map(x => ({ label: x.name || x.serialNumber || `FDH-${x.id}`, value: x.id, meta: x })));
      }
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Failed to load options', severity: 'error' });
    } finally {
      setLoadingChildren(false);
      setLoadingParents(false);
    }
  };

  const handleConfirm = async () => {
    if (!child || !parent) return;
    setSubmitting(true);
    try {
      let resp;
      if (deviceType === 'CORE_SWITCH') {
        resp = await inventoryService.reparentCoreSwitch(child.value, parent.value);
      } else if (deviceType === 'FDH') {
        resp = await inventoryService.reparentFdh(child.value, parent.value);
      } else if (deviceType === 'SPLITTER') {
        resp = await inventoryService.reparentSplitter(child.value, parent.value);
      }
      setSnack({ open: true, message: 'Move successful', severity: 'success' });
      // refresh lists
      await loadChildOptions(deviceType);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || e.message || 'Move failed', severity: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  if (!canAccess) return (
    <>
      <Header />
      <Container sx={{ pt: 12 }}>
        <Typography variant="h6">You do not have permission to view the Topology Editor. Admins or Planners only.</Typography>
      </Container>
    </>
  );

  return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
        <Typography variant="h4" sx={{ mb: 2 }}>Network Topology Editor</Typography>

        <Paper sx={{ p: 3, mb: 3 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Box>
              <FormControl fullWidth size="small">
                <InputLabel id="device-type-label">Device Type</InputLabel>
                <Select
                  labelId="device-type-label"
                  value={deviceType}
                  label="Device Type"
                  onChange={(e) => setDeviceType(e.target.value)}
                  sx={{ minWidth: 240 }}
                >
                  {deviceTypes.map(d => <MenuItem key={d.value} value={d.value}>{d.label}</MenuItem>)}
                </Select>
              </FormControl>
            </Box>

            <Box>
              {loadingChildren ? <CircularProgress size={28} /> : (
                <Autocomplete
                  options={childOptions}
                  getOptionLabel={(o) => o.label || ''}
                  value={child}
                  onChange={(e, v) => setChild(v)}
                  sx={{ width: '100%' }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Select device to move"
                      fullWidth
                      size="small"
                      sx={{ '& .MuiInputBase-input': { fontSize: '0.95rem' } }}
                    />
                  )}
                />
              )}
            </Box>

            <Box>
              {loadingParents ? <CircularProgress size={28} /> : (
                <Autocomplete
                  options={parentOptions}
                  getOptionLabel={(o) => o.label || ''}
                  value={parent}
                  onChange={(e, v) => setParent(v)}
                  sx={{ width: '100%' }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Select new parent"
                      fullWidth
                      size="small"
                      sx={{ '& .MuiInputBase-input': { fontSize: '0.95rem' } }}
                    />
                  )}
                />
              )}
            </Box>

            <Box sx={{ display: 'flex', gap: 2, mt: 1 }}>
              <Button variant="contained" color="primary" disabled={!child || !parent || submitting} onClick={handleConfirm}>
                {submitting ? 'Moving...' : 'Confirm Move'}
              </Button>
              <Button variant="outlined" onClick={() => { setChild(null); setParent(null); }}>Reset</Button>
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

export default TopologyEditor;
