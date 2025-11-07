import React, { useEffect, useState, useContext } from 'react';
import { Container, Box, Paper, Typography, Grid, Button, CircularProgress, Snackbar, Alert } from '@mui/material';
import Header from '../components/Header';
import { AuthContext } from '../context/AuthContext';
import customerService from '../services/customerService';
import inventoryService from '../services/inventoryService';
import deploymentService from '../services/deploymentService';
import PeopleIcon from '@mui/icons-material/People';
import StorageIcon from '@mui/icons-material/Storage';
import DevicesIcon from '@mui/icons-material/Devices';
import AssignmentIcon from '@mui/icons-material/Assignment';
import AssessmentIcon from '@mui/icons-material/Assessment';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import { Link as RouterLink } from 'react-router-dom';

const StatCard = ({ title, value, icon: Icon, sx = {} }) => (
  <Paper sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2, ...sx }}>
    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 56, height: 56, borderRadius: 1, bgcolor: (t) => t.palette.action.hover }}>
      <Icon />
    </Box>
    <Box>
      <Typography variant="subtitle2">{title}</Typography>
      <Typography variant="h5">{typeof value === 'number' ? value : value}</Typography>
    </Box>
  </Paper>
);

const AdminDashboard = () => {
  const { user } = useContext(AuthContext);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({});
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });

  useEffect(() => {
    if (!user || !user.roles?.includes('ROLE_ADMIN')) return;
    loadStats();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const loadStats = async () => {
    setLoading(true);
    try {
      // customers
      const customers = await customerService.getAllCustomers();
      const totalCustomers = Array.isArray(customers) ? customers.length : 0;
      const pendingCustomers = Array.isArray(customers) ? customers.filter(c => c.status === 'PENDING').length : 0;
      const activeCustomers = Array.isArray(customers) ? customers.filter(c => c.status === 'ACTIVE').length : 0;

      // assets
      const available = await inventoryService.getAssets({ status: 'AVAILABLE' }).catch(() => []);
      const assigned = await inventoryService.getAssets({ status: 'ASSIGNED' }).catch(() => []);
      const faultyAssigned = await inventoryService.getFaultyAssigned().catch(() => []);

      // tasks
      const tasks = await deploymentService.getTasks().catch(() => []);
      const openTasks = Array.isArray(tasks) ? tasks.filter(t => t.status !== 'COMPLETED').length : 0;

      setStats({
        totalCustomers,
        pendingCustomers,
        activeCustomers,
        availableAssets: Array.isArray(available) ? available.length : 0,
        assignedAssets: Array.isArray(assigned) ? assigned.length : 0,
        faultyAssets: Array.isArray(faultyAssigned) ? faultyAssigned.length : 0,
        openTasks,
        recentTasks: Array.isArray(tasks) ? tasks.slice(0, 5) : [],
      });
    } catch (e) {
      setSnack({ open: true, message: e?.response?.data?.message || 'Failed to load admin stats', severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!user || !user.roles?.includes('ROLE_ADMIN')) {
    return (
      <>
        <Header />
        <Container sx={{ pt: 12 }}>
          <Typography variant="h6">Access denied. Administrator role required.</Typography>
        </Container>
      </>
    );
  }

  return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Box>
            <Typography variant="h4">Admin Panel</Typography>
            <Typography variant="body2" color="text.secondary">
              Signed in as: {user?.username}{' '}
              {user?.roles && user.roles.length > 0 ? `(${user.roles.map(r => r.replace(/^ROLE_/, '')).join(', ')})` : ''}
            </Typography>
          </Box>
          <Box>
            <Button component={RouterLink} to="/admin/users" variant="outlined" startIcon={<AdminPanelSettingsIcon /> } sx={{ mr: 1 }}>Users</Button>
            <Button component={RouterLink} to="/admin/audit-logs" variant="outlined" startIcon={<AssignmentIcon />}>Audit Logs</Button>
          </Box>
        </Box>

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress /></Box>
        ) : (
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Total Customers" value={stats.totalCustomers ?? 0} icon={PeopleIcon} />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Pending Customers" value={stats.pendingCustomers ?? 0} icon={PeopleIcon} />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Active Customers" value={stats.activeCustomers ?? 0} icon={PeopleIcon} />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Open Tasks" value={stats.openTasks ?? 0} icon={AssignmentIcon} />
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Available Assets" value={stats.availableAssets ?? 0} icon={StorageIcon} />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Assigned Assets" value={stats.assignedAssets ?? 0} icon={DevicesIcon} />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Faulty Assets" value={stats.faultyAssets ?? 0} icon={DevicesIcon} />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="subtitle2">Quick Actions</Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mt: 1 }}>
                  <Button component={RouterLink} to="/customers" variant="contained">Manage Customers</Button>
                  <Button component={RouterLink} to="/inventory" variant="outlined">Inventory</Button>
                  <Button component={RouterLink} to="/deployments/new" variant="outlined">Create Deployment Task</Button>
                  <Button component={RouterLink} to="/replace-assets" variant="outlined">Replace Asset</Button>
                </Box>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6">Recent Tasks</Typography>
                {stats.recentTasks && stats.recentTasks.length === 0 && <Typography>No recent tasks</Typography>}
                {stats.recentTasks && stats.recentTasks.map(t => (
                  <Paper key={t.id} sx={{ p: 1, mt: 1 }}>
                    <Typography><strong>{t.title || `Task ${t.id}`}</strong> — {t.status}</Typography>
                    <Typography variant="caption">Scheduled: {t.scheduledDate ? new Date(t.scheduledDate).toLocaleString() : '-'}</Typography>
                  </Paper>
                ))}
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6">Insights</Typography>
                <Typography variant="body2" sx={{ mt: 1 }}>Heatmap, SLA breaches and alerts will appear here (backend endpoints not yet available).</Typography>
              </Paper>
            </Grid>
          </Grid>
        )}

        <Snackbar open={snack.open} autoHideDuration={6000} onClose={() => setSnack(s => ({ ...s, open: false }))}>
          <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.message}</Alert>
        </Snackbar>
      </Container>
    </>
  );
};

export default AdminDashboard;
