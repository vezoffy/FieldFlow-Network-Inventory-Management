import React, { useEffect, useState, useContext } from 'react';
import {
  Container,
  Paper,
  Box,
  Typography,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Snackbar,
  Alert,
  Grid,
  Stepper,
  Step,
  StepLabel,
} from '@mui/material';
import Autocomplete from '@mui/material/Autocomplete';
import Header from '../components/Header';
import { AuthContext } from '../context/AuthContext';
import customerService from '../services/customerService';
import inventoryService from '../services/inventoryService';
import deploymentService from '../services/deploymentService';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';

const steps = ['Assign Hardware', 'Assign Network Port', 'Create Installation Task'];

const Onboarding = () => {
  const { user } = useContext(AuthContext);
  const [pending, setPending] = useState([]);
  const [loadingPending, setLoadingPending] = useState(false);

  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [customerDetails, setCustomerDetails] = useState(null);
  const [loadingCustomer, setLoadingCustomer] = useState(false);

  // Assign Hardware dialog
  const [assignOpen, setAssignOpen] = useState(false);
  const [availableAssets, setAvailableAssets] = useState([]);
  const [chosenAssetSerial, setChosenAssetSerial] = useState('');
  const [loadingAssets, setLoadingAssets] = useState(false);

  // Assign Port dialog
  const [portOpen, setPortOpen] = useState(false);
  const [splitters, setSplitters] = useState([]);
  const [selectedSplitter, setSelectedSplitter] = useState(null);
  const [portNumber, setPortNumber] = useState('');
  const [lengthMeters, setLengthMeters] = useState('');
  const [loadingSplitters, setLoadingSplitters] = useState(false);

  // Schedule dialog
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [technicians, setTechnicians] = useState([]);
  const [selectedTech, setSelectedTech] = useState(null);
  const [scheduledDate, setScheduledDate] = useState('');
  const [loadingTechs, setLoadingTechs] = useState(false);

  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });

  useEffect(() => {
    if (!user) return;
    loadPendingCustomers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const loadPendingCustomers = async () => {
    setLoadingPending(true);
    try {
      const list = await customerService.searchCustomers({ status: 'PENDING' });
      setPending(Array.isArray(list) ? list : []);
    } catch (e) {
      setSnack({ open: true, message: 'Failed to load pending customers', severity: 'error' });
    } finally {
      setLoadingPending(false);
    }
  };

  const loadCustomerDetails = async (id) => {
    if (!id) return;
    setLoadingCustomer(true);
    try {
      const data = await customerService.getCustomer(id);
      setCustomerDetails(data);
    } catch (e) {
      setSnack({ open: true, message: 'Failed to load customer details', severity: 'error' });
    } finally {
      setLoadingCustomer(false);
    }
  };

  const openAssignDialog = async () => {
    setAssignOpen(true);
    setChosenAssetSerial('');
    setAvailableAssets([]);
    setLoadingAssets(true);
    try {
      const opts = await inventoryService.getAssets({ status: 'AVAILABLE' });
      setAvailableAssets(Array.isArray(opts) ? opts : []);
    } catch (e) {
      setSnack({ open: true, message: 'Failed to load available assets', severity: 'error' });
    } finally {
      setLoadingAssets(false);
    }
  };

  const submitAssign = async () => {
    if (!customerDetails || !chosenAssetSerial) {
      setSnack({ open: true, message: 'Please select an asset to assign.', severity: 'error' });
      return;
    }
    try {
      await customerService.assignAsset(customerDetails.id, chosenAssetSerial);
      setSnack({ open: true, message: 'Asset assigned successfully', severity: 'success' });
      await loadCustomerDetails(customerDetails.id);
      await loadPendingCustomers();
      setAssignOpen(false);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || 'Failed to assign asset', severity: 'error' });
    }
  };

  const openPortDialog = async () => {
    setPortOpen(true);
    setSelectedSplitter(null);
    setPortNumber('');
    setLengthMeters('');
    setSplitters([]);
    setLoadingSplitters(true);
    try {
      const s = await inventoryService.getSplitters();
      setSplitters(Array.isArray(s) ? s : []);
    } catch (e) {
      setSnack({ open: true, message: 'Failed to load splitters', severity: 'error' });
    } finally {
      setLoadingSplitters(false);
    }
  };

  const submitPort = async () => {
    if (!customerDetails || !selectedSplitter || !portNumber) {
      setSnack({ open: true, message: 'Please choose a splitter and enter a port number', severity: 'error' });
      return;
    }
    try {
      const body = {
        splitterSerialNumber: selectedSplitter.serialNumber || selectedSplitter.serial || selectedSplitter.id,
        portNumber: Number(portNumber),
        lengthMeters: lengthMeters ? Number(lengthMeters) : 0.0,
      };
      await customerService.assignPort(customerDetails.id, body);
      setSnack({ open: true, message: 'Port assigned successfully', severity: 'success' });
      await loadCustomerDetails(customerDetails.id);
      await loadPendingCustomers();
      setPortOpen(false);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || 'Failed to assign port', severity: 'error' });
    }
  };

  const openScheduleDialog = async () => {
    setScheduleOpen(true);
    setSelectedTech(null);
    setScheduledDate('');
    setTechnicians([]);
    setLoadingTechs(true);
    try {
      const t = await deploymentService.getTechnicians();
      setTechnicians(Array.isArray(t) ? t : []);
    } catch (e) {
      setSnack({ open: true, message: 'Failed to load technicians', severity: 'error' });
    } finally {
      setLoadingTechs(false);
    }
  };

  const submitSchedule = async () => {
    if (!customerDetails || !selectedTech || !scheduledDate) {
      setSnack({ open: true, message: 'Please select a technician and date', severity: 'error' });
      return;
    }
    try {
      const body = {
        customerId: customerDetails.id,
        technicianId: selectedTech.id || selectedTech.techId || selectedTech.id,
        scheduledDate,
      };
      await deploymentService.createTask(body);
      setSnack({ open: true, message: 'Installation task created successfully!', severity: 'success' });
      // Onboarding from planner perspective is complete; refresh list
      await loadPendingCustomers();
      setScheduleOpen(false);
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || 'Failed to create task', severity: 'error' });
    }
  };

  const roles = user?.roles || [];
  const canAccess = roles.some(r => ['ROLE_PLANNER', 'ROLE_ADMIN'].includes(r));

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
            <AutoFixHighIcon />
            <Typography variant="h5">Customer Onboarding</Typography>
          </Box>

          <Box sx={{ mb: 2 }}>
            <Typography variant="subtitle1">Pending Customers</Typography>
            {loadingPending ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}><CircularProgress /></Box>
            ) : (
              <Autocomplete
                options={pending}
                getOptionLabel={(o) => `${o.name || o.fullName || o.customerName || o.username} (ID: ${o.id})`}
                value={selectedCustomer}
                onChange={(e, v) => { setSelectedCustomer(v); setCustomerDetails(null); loadCustomerDetails(v ? v.id : null); }}
                renderInput={(params) => <TextField {...params} label="Select pending customer to onboard" />}
                isOptionEqualToValue={(o, v) => o.id === v.id}
              />
            )}
          </Box>

          {selectedCustomer && (
            <Box sx={{ mt: 2 }}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6">Onboarding: {customerDetails ? (customerDetails.name || customerDetails.fullName || customerDetails.customerName) : selectedCustomer.name} (ID: {selectedCustomer.id})</Typography>
                <Box sx={{ mt: 2 }}>
                  <Stepper
                    activeStep={
                      customerDetails
                        ? (
                            (customerDetails.assignedAssets && customerDetails.assignedAssets.length ? 1 : 0) +
                            (customerDetails.splitterId && customerDetails.assignedPort ? 1 : 0)
                          )
                        : 0
                    }
                    alternativeLabel
                  >
                    {steps.map((label) => (
                      <Step key={label}><StepLabel>{label}</StepLabel></Step>
                    ))}
                  </Stepper>

                  <Box sx={{ mt: 2 }}>
                    {/* Step 1 - Assign Hardware */}
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2">Step 1 — Assigned Hardware</Typography>
                      {loadingCustomer ? (
                        <CircularProgress />
                      ) : (
                        <Box>
                          {customerDetails?.assignedAssets && customerDetails.assignedAssets.length > 0 ? (
                            customerDetails.assignedAssets.map(a => (
                              <Paper key={a.serialNumber || a.id} sx={{ p: 1, mt: 1 }}>
                                <Typography><strong>{a.assetType}</strong> — {a.serialNumber}</Typography>
                                <Typography variant="caption">Model: {a.model || '-'}</Typography>
                              </Paper>
                            ))
                          ) : (
                            <Typography>No assets assigned yet.</Typography>
                          )}
                          <Box sx={{ mt: 1 }}>
                            <Button variant="outlined" onClick={openAssignDialog}>Assign New Asset</Button>
                          </Box>
                        </Box>
                      )}
                    </Box>

                    {/* Step 2 - Assign Port */}
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2">Step 2 — Network Port</Typography>
                      {loadingCustomer ? (
                        <CircularProgress />
                      ) : (
                        <Box>
                          <Typography>Splitter: {customerDetails?.splitterId ? (customerDetails.splitterId) : 'Not Assigned'}</Typography>
                          <Typography>Port: {customerDetails?.assignedPort ?? 'Not Assigned'}</Typography>
                          <Box sx={{ mt: 1 }}>
                            <Button variant="outlined" onClick={openPortDialog}>Assign Port</Button>
                          </Box>
                        </Box>
                      )}
                    </Box>

                    {/* Step 3 - Create Installation Task */}
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2">Step 3 — Installation Task</Typography>
                      <Typography variant="body2">Schedule a technician after hardware and port are assigned.</Typography>
                      <Box sx={{ mt: 1 }}>
                        <Button variant="contained" onClick={openScheduleDialog} disabled={!customerDetails || !(customerDetails?.assignedAssets && customerDetails.assignedAssets.length) || !(customerDetails?.splitterId && customerDetails?.assignedPort)}>Schedule Installation</Button>
                      </Box>
                    </Box>
                  </Box>
                </Box>
              </Paper>
            </Box>
          )}
        </Paper>

        {/* Assign Asset Dialog */}
        <Dialog open={assignOpen} onClose={() => setAssignOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>Assign Asset to Customer</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 1 }}>
              {loadingAssets ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress /></Box>
              ) : (
                <Autocomplete
                  options={availableAssets}
                  getOptionLabel={(o) => `${o.assetType || o.type} — ${o.serialNumber || o.serial || o.id}`}
                  onChange={(e, v) => setChosenAssetSerial(v ? (v.serialNumber || v.serial) : '')}
                  renderInput={(params) => <TextField {...params} label="Select available asset" />}
                />
              )}
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAssignOpen(false)}>Cancel</Button>
            <Button variant="contained" onClick={submitAssign}>Assign</Button>
          </DialogActions>
        </Dialog>

        {/* Assign Port Dialog */}
        <Dialog open={portOpen} onClose={() => setPortOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>Assign Network Port</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 1, display: 'grid', gap: 2 }}>
              {loadingSplitters ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress /></Box>
              ) : (
                <Autocomplete
                  options={splitters}
                  getOptionLabel={(s) => `${s.serialNumber || s.id} — ${s.location || s.name || ''}`}
                  onChange={(e, v) => setSelectedSplitter(v)}
                  renderInput={(params) => <TextField {...params} label="Select Splitter" />}
                />
              )}

              <TextField label="Port Number" value={portNumber} onChange={(e) => setPortNumber(e.target.value)} />
              <TextField label="Length (meters)" value={lengthMeters} onChange={(e) => setLengthMeters(e.target.value)} />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setPortOpen(false)}>Cancel</Button>
            <Button variant="contained" onClick={submitPort}>Assign Port</Button>
          </DialogActions>
        </Dialog>

        {/* Schedule Dialog */}
        <Dialog open={scheduleOpen} onClose={() => setScheduleOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>Schedule Installation</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 1, display: 'grid', gap: 2 }}>
              {loadingTechs ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress /></Box>
              ) : (
                <Autocomplete
                  options={technicians}
                  getOptionLabel={(t) => `${t.name || t.fullName || t.username || t.id} ${t.id ? `(ID: ${t.id})` : ''}`}
                  onChange={(e, v) => setSelectedTech(v)}
                  renderInput={(params) => <TextField {...params} label="Select Technician" />}
                />
              )}

              <TextField
                label="Scheduled Date"
                type="date"
                value={scheduledDate}
                onChange={(e) => setScheduledDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setScheduleOpen(false)}>Cancel</Button>
            <Button variant="contained" onClick={submitSchedule}>Create Task</Button>
          </DialogActions>
        </Dialog>

        <Snackbar open={snack.open} autoHideDuration={6000} onClose={() => setSnack(s => ({ ...s, open: false }))}>
          <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.message}</Alert>
        </Snackbar>
      </Container>
    </>
  );
};

export default Onboarding;
