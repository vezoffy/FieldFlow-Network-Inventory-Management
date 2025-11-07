import React, { useContext, useState } from 'react';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import Typography from '@mui/material/Typography';
import Avatar from '@mui/material/Avatar';
import Logo from './Logo';
import { Link as RouterLink } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import HistoryIcon from '@mui/icons-material/History';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import BuildIcon from '@mui/icons-material/Build';
import SupportAgentIcon from '@mui/icons-material/SupportAgent';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import PeopleIcon from '@mui/icons-material/People';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import SyncAltIcon from '@mui/icons-material/SyncAlt';
import LogoutIcon from '@mui/icons-material/Logout';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import { ColorModeContext } from '../context/ColorModeContext';

const Header = () => {
  const { user, logout } = useContext(AuthContext);
  const colorMode = useContext(ColorModeContext);
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const handleOpen = (e) => setAnchorEl(e.currentTarget);
  const handleClose = () => setAnchorEl(null);

  // Show role (not username) in the header per requirements. Use first role if available.
  const primaryRole = user?.roles && user.roles.length > 0 ? user.roles[0] : null;
  const roleLabel = primaryRole ? primaryRole.replace(/^ROLE_/, '').toLowerCase().split('_').map(s => s[0].toUpperCase() + s.slice(1)).join(' ') : '';
  const initials = roleLabel ? roleLabel.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase() : '';

  return (
    <AppBar position="absolute" color="transparent" elevation={0} sx={{ zIndex: 10 }}>
      <Container maxWidth="lg">
        <Toolbar disableGutters>
          <Box sx={{ flexGrow: 1 }}>
            <Logo height={100} />
          </Box>
          {/* Color mode toggle */}
          <Box sx={{ mr: 2 }}>
            <IconButton sx={{ ml: 1 }} onClick={() => colorMode.toggleColorMode()} color="inherit">
              {colorMode.mode === 'dark' ? <Brightness7Icon /> : <Brightness4Icon />}
            </IconButton>
          </Box>

          {!user && (
            <Button
              variant="contained"
              color="primary"
              component={RouterLink}
              to="/login"
            >
              Login
            </Button>
          )}

          {user && (
            <>
              <Button
                onClick={handleOpen}
                startIcon={<Avatar sx={{ width: 32, height: 32 }}>{initials}</Avatar>}
                sx={{ textTransform: 'none' }}
              >
                <Typography variant="body1" sx={{ ml: 1 }}>
                  {roleLabel || user.username}
                </Typography>
              </Button>

              <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={handleClose}
                keepMounted
                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                PaperProps={{ sx: { minWidth: { xs: 260, sm: 360 }, maxWidth: 520 } }}
              >
                {user.roles?.includes('ROLE_ADMIN') && (
                  <MenuItem component={RouterLink} to="/admin" onClick={handleClose}>
                    <ListItemIcon>
                      <AdminPanelSettingsIcon fontSize="small" />
                    </ListItemIcon>
                    Admin Panel
                  </MenuItem>
                )}

                {user.roles?.includes('ROLE_ADMIN') && (
                  <MenuItem component={RouterLink} to="/admin/audit-logs" onClick={handleClose}>
                    <ListItemIcon>
                      <HistoryIcon fontSize="small" />
                    </ListItemIcon>
                    Audit Logs
                  </MenuItem>
                )}

                {(user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ROLE_PLANNER')) && (
                  <MenuItem component={RouterLink} to="/topology-editor" onClick={handleClose}>
                    <ListItemIcon>
                      <SwapHorizIcon fontSize="small" />
                    </ListItemIcon>
                    Topology Editor
                  </MenuItem>
                )}

                {user.roles?.includes('ROLE_ADMIN') && (
                  <MenuItem component={RouterLink} to="/admin/users" onClick={handleClose}>
                    <ListItemIcon>
                      <AdminPanelSettingsIcon fontSize="small" />
                    </ListItemIcon>
                    User Management
                  </MenuItem>
                )}

                {(user.roles?.includes('ROLE_PLANNER') || user.roles?.includes('ROLE_ADMIN')) && (
                  <MenuItem component={RouterLink} to="/onboarding" onClick={handleClose}>
                    <ListItemIcon>
                      <AutoFixHighIcon fontSize="small" />
                    </ListItemIcon>
                    Onboarding Wizard
                  </MenuItem>
                )}

                {(user.roles?.includes('ROLE_TECHNICIAN') || user.roles?.includes('ROLE_ADMIN')) && (
                  <MenuItem component={RouterLink} to="/tasks" onClick={handleClose}>
                    <ListItemIcon>
                      <BuildIcon fontSize="small" />
                    </ListItemIcon>
                    Tasks
                  </MenuItem>
                )}

                {user.roles?.includes('ROLE_SUPPORT_AGENT') && (
                  <MenuItem component={RouterLink} to="/support" onClick={handleClose}>
                    <ListItemIcon>
                      <SupportAgentIcon fontSize="small" />
                    </ListItemIcon>
                    Support Portal
                  </MenuItem>
                )}

                {/* Inventory dashboard link - visible to Planner, Admin, Technician */}
                {(user.roles?.includes('ROLE_PLANNER') || user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ROLE_TECHNICIAN')) && (
                  <MenuItem component={RouterLink} to="/inventory" onClick={handleClose}>
                    <ListItemIcon>
                      <Inventory2Icon fontSize="small" />
                    </ListItemIcon>
                    Inventory
                  </MenuItem>
                )}

                {(user.roles?.includes('ROLE_PLANNER') || user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ROLE_SUPPORT_AGENT') || user.roles?.includes('ROLE_TECHNICIAN')) && (
                  <MenuItem component={RouterLink} to="/customers" onClick={handleClose}>
                    <ListItemIcon>
                      <PeopleIcon fontSize="small" />
                    </ListItemIcon>
                    Customers
                  </MenuItem>
                )}

                {(user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ROLE_SUPPORT_AGENT')) && (
                  <MenuItem component={RouterLink} to="/deactivate-customer" onClick={handleClose}>
                    <ListItemIcon>
                      <SupportAgentIcon fontSize="small" />
                    </ListItemIcon>
                    Deactivate Customer
                  </MenuItem>
                )}

                {(user.roles?.includes('ROLE_TECHNICIAN') || user.roles?.includes('ROLE_ADMIN')) && (
                  <MenuItem component={RouterLink} to="/replace-assets" onClick={handleClose}>
                    <ListItemIcon>
                      <SyncAltIcon fontSize="small" />
                    </ListItemIcon>
                    Replace Asset
                  </MenuItem>
                )}

                {(user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ROLE_TECHNICIAN')) && (
                  <MenuItem component={RouterLink} to="/deployments/new" onClick={handleClose}>
                    <ListItemIcon>
                      <BuildIcon fontSize="small" />
                    </ListItemIcon>
                    Create Deployment Task
                  </MenuItem>
                )}

                {/* Network Topology - visible to all logged in users */}
                {(user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ROLE_PLANNER')) && (
                  <MenuItem component={RouterLink} to="/topology" onClick={handleClose}>
                    <ListItemIcon>
                      <AccountTreeIcon fontSize="small" />
                    </ListItemIcon>
                    Network Topology
                  </MenuItem>
                )}

                <MenuItem
                  onClick={() => {
                    handleClose();
                    logout();
                  }}
                >
                  <ListItemIcon>
                    <LogoutIcon fontSize="small" />
                  </ListItemIcon>
                  Logout
                </MenuItem>
              </Menu>
            </>
          )}
        </Toolbar>
      </Container>
    </AppBar>
  );
};

export default Header;