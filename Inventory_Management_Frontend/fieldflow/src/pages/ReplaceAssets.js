import React, { useEffect, useState, useContext } from 'react';
import { Container, Paper, Box, Typography, TextField, Button, Grid, Dialog, DialogTitle, DialogContent, DialogActions, IconButton, CircularProgress, Snackbar, Alert } from '@mui/material';
import Autocomplete from '@mui/material/Autocomplete';
import Header from '../components/Header';
import { AuthContext } from '../context/AuthContext';
import customerService from '../services/customerService';
import inventoryService from '../services/inventoryService';
import SyncAltIcon from '@mui/icons-material/SyncAlt';
import CloseIcon from '@mui/icons-material/Close';

const ReplaceAssets = () => {
  const { user } = useContext(AuthContext);
  const [faultyAssets, setFaultyAssets] = useState([]);
  const [loadingFaulty, setLoadingFaulty] = useState(false);

  const [replaceOpen, setReplaceOpen] = useState(false);
  const [oldAsset, setOldAsset] = useState(null);
  const [replacementOptions, setReplacementOptions] = useState([]);
  const [replacementSerial, setReplacementSerial] = useState('');
  const [loadingOptions, setLoadingOptions] = useState(false);

  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });

  useEffect(() => {
    if (!user) return;
    loadFaultyAssets();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const loadFaultyAssets = async () => {
    setLoadingFaulty(true);
    try {
      const list = await inventoryService.getFaultyAssigned();
      setFaultyAssets(Array.isArray(list) ? list : []);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || 'Failed to load faulty assets', severity: 'error' });
    } finally {
      setLoadingFaulty(false);
    }
  };

  const openReplace = async (asset) => {
    setOldAsset(asset);
    setReplacementSerial('');
    setReplacementOptions([]);
    setReplaceOpen(true);
    // load available replacements of same type (ensure same assetType)
    setLoadingOptions(true);
    try {
      const type = asset.assetType || asset.type;
      const opts = await inventoryService.getAssets({ status: 'AVAILABLE', type });
      setReplacementOptions(Array.isArray(opts) ? opts : []);
    } catch (e) {
      setSnack({ open: true, message: 'Failed to load replacement assets', severity: 'error' });
    } finally {
      setLoadingOptions(false);
    }
  };

  const submitReplace = async () => {
    if (!oldAsset || !replacementSerial) {
      setSnack({ open: true, message: 'Please select a faulty asset and a replacement', severity: 'error' });
      return;
    }

    try {
      // call atomic replace endpoint
      const resp = await inventoryService.replaceAsset(oldAsset.serialNumber || oldAsset.serial, replacementSerial);
      setSnack({ open: true, message: 'Replacement completed', severity: 'success' });
      // refresh faulty list
      await loadFaultyAssets();
      setReplaceOpen(false);
    } catch (e) {
      const code = e?.response?.status;
      if (code === 404) {
        setSnack({ open: true, message: 'One of the serial numbers was not found (404).', severity: 'error' });
      } else if (code === 409) {
        setSnack({ open: true, message: 'Replacement asset is not AVAILABLE (409). Please pick another asset.', severity: 'error' });
      } else {
        setSnack({ open: true, message: e?.response?.data?.message || 'Replacement failed', severity: 'error' });
      }
    }
  };

  const roles = user?.roles || [];
  const canAccess = roles.some(r => ['ROLE_TECHNICIAN','ROLE_ADMIN'].includes(r));

  if (!canAccess) {
    return (
      <>
        <Header />
        <Container sx={{ pt: 12 }}>
          <Typography variant="h6">Access denied. Insufficient role.</Typography>
        </Container>
      </>
    );
  }

  return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
            <SyncAltIcon />
            <Typography variant="h5">Replace Faulty ONT / Router</Typography>
          </Box>

          <Box sx={{ mb: 2 }}>
            <Typography variant="subtitle1">Faulty Assigned Assets</Typography>
            {loadingFaulty ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}><CircularProgress /></Box>
            ) : (
              <Box>
                {faultyAssets.length === 0 && <Typography>No faulty assigned ONTs or routers found.</Typography>}
                {faultyAssets.map(a => (
                  <Paper key={a.serialNumber || a.id} sx={{ p: 1, mt: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Box>
                      <Typography variant="body2"><strong>{a.assetType}</strong> — {a.serialNumber}</Typography>
                      <Typography variant="caption">Model: {a.model || '-' } — Assigned to customer: {a.assignedToCustomerId}</Typography>
                    </Box>
                    <Box>
                      <Button variant="outlined" onClick={() => openReplace(a)}>Replace</Button>
                    </Box>
                  </Paper>
                ))}
              </Box>
            )}
          </Box>
        </Paper>

        <Dialog open={replaceOpen} onClose={() => setReplaceOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>Replace Asset</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 1, display: 'grid', gap: 2 }}>
              <Typography>Old: {oldAsset?.type || oldAsset?.assetType} — {oldAsset?.serialNumber || oldAsset?.serial}</Typography>

              {loadingOptions ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress /></Box>
              ) : (
                <Autocomplete
                  options={replacementOptions}
                  getOptionLabel={(opt) => opt.serialNumber || opt.serial || `${opt.id} — ${opt.serialNumber || opt.serial}`}
                  value={replacementOptions.find(x => (x.serialNumber || x.serial) === replacementSerial) || null}
                  onChange={(e, v) => setReplacementSerial(v ? (v.serialNumber || v.serial) : '')}
                  renderInput={(params) => <TextField {...params} label="Select replacement asset (available)" />}
                />
              )}
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setReplaceOpen(false)}>Cancel</Button>
            <Button variant="contained" onClick={submitReplace}>Replace</Button>
          </DialogActions>
        </Dialog>

        <Snackbar open={snack.open} autoHideDuration={6000} onClose={() => setSnack(s => ({ ...s, open: false }))}>
          <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.message}</Alert>
        </Snackbar>
      </Container>
    </>
  );
};

export default ReplaceAssets;
