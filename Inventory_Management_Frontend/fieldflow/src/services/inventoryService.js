import api from '../api/axiosInstance';

// Asset helpers
const getAssets = async (params = {}) => {
  const resp = await api.get('/api/inventory/assets', { params });
  return resp.data;
};

const createAsset = async (body) => {
  const resp = await api.post('/api/inventory/assets', body);
  return resp.data;
};

const getAsset = async (serialNumber) => {
  const resp = await api.get(`/api/inventory/assets/${encodeURIComponent(serialNumber)}`);
  return resp.data;
};

const updateStatus = async (id, newStatus) => {
  const resp = await api.patch(`/api/inventory/assets/${id}/status`, { newStatus });
  return resp.data;
};

const getHistory = async (id) => {
  const resp = await api.get(`/api/inventory/assets/${id}/history`);
  return resp.data;
};

// Headend / CoreSwitch / FDH / Splitter list & get
const getHeadends = async () => {
  const resp = await api.get('/api/inventory/headends');
  return resp.data;
};

const getHeadend = async (id) => {
  const resp = await api.get(`/api/inventory/headends/${id}`);
  return resp.data;
};

const getCoreSwitches = async () => {
  const resp = await api.get('/api/inventory/core-switches');
  return resp.data;
};

const getCoreSwitch = async (id) => {
  const resp = await api.get(`/api/inventory/core-switches/${id}`);
  return resp.data;
};

const getFdhs = async () => {
  const resp = await api.get('/api/inventory/fdhs');
  return resp.data;
};

const getFdh = async (id) => {
  const resp = await api.get(`/api/inventory/fdhs/${id}`);
  return resp.data;
};

const getSplitters = async () => {
  const resp = await api.get('/api/inventory/splitters');
  return resp.data;
};

const getSplitter = async (id) => {
  const resp = await api.get(`/api/inventory/splitters/${id}`);
  return resp.data;
};

// Fiber drop lines (customer-facing fiber rolls)
const getFiberDropLines = async () => {
  const resp = await api.get('/api/customers/fiber-drop-lines');
  return resp.data;
};

const getFiberDropLinesBySplitter = async (splitterId) => {
  const resp = await api.get(`/api/customers/fiber-drop-lines/splitter/${splitterId}`);
  return resp.data;
};

// Reparenting endpoints
const reparentCoreSwitch = async (id, newHeadendId) => {
  const resp = await api.patch(`/api/inventory/core-switches/${id}/reparent`, { newHeadendId });
  return resp.data;
};

const reparentFdh = async (id, newCoreSwitchId) => {
  const resp = await api.patch(`/api/inventory/fdhs/${id}/reparent`, { newCoreSwitchId });
  return resp.data;
};

const reparentSplitter = async (id, newFdhId) => {
  const resp = await api.patch(`/api/inventory/splitters/${id}/reparent`, { newFdhId });
  return resp.data;
};

const inventoryService = {
  // assets
  getAssets,
  createAsset,
  getAsset,
  updateStatus,
  getHistory,
  // lists & gets
  getHeadends,
  getHeadend,
  getCoreSwitches,
  getCoreSwitch,
  getFdhs,
  getFdh,
  getSplitters,
  getSplitter,
  // fiber
  getFiberDropLines,
  getFiberDropLinesBySplitter,
  // reparenting
  reparentCoreSwitch,
  reparentFdh,
  reparentSplitter,
  // asset helpers
  updateAsset: async (id, body) => {
    const resp = await api.put(`/api/inventory/assets/${id}`, body);
    return resp.data;
  },
  unassignBySerial: async (serialNumber) => {
    const resp = await api.post(`/api/inventory/assets/unassign/by-serial/${encodeURIComponent(serialNumber)}`);
    return resp.data;
  },
  getFaultyAssigned: async () => {
    const resp = await api.get('/api/inventory/assets/faulty-assigned');
    return resp.data;
  },
  replaceAsset: async (faultySerial, newSerial) => {
    const body = {
      faultyAssetSerialNumber: faultySerial,
      newAssetSerialNumber: newSerial,
    };
    const resp = await api.post('/api/inventory/assets/replace', body);
    return resp.data;
  },
  deleteAsset: async (id) => {
    const resp = await api.delete(`/api/inventory/assets/by-id/${id}`);
    return resp;
  },
};

export default inventoryService;
