import React from 'react';
import { CssBaseline } from '@mui/material';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import InventoryDashboard from './pages/InventoryDashboard';
import NetworkTopology from './pages/NetworkTopology';
import { AuthProvider } from './context/AuthContext';
import ColorModeProvider from './context/ColorModeContext';

function App() {
  return (
    <ColorModeProvider>
      <CssBaseline />
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/inventory" element={<InventoryDashboard />} />
            <Route path="/topology" element={<NetworkTopology />} />
            <Route path="/" element={<LandingPage />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ColorModeProvider>
  );
}

export default App;