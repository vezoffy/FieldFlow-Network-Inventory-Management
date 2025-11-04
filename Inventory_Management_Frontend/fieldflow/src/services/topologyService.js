import api from '../api/axiosInstance';

const topologyService = {
  getCustomerPath: async (customerId) => {
    const resp = await api.get(`/api/topology/customer/${customerId}`);
    return resp.data;
  },
  getInfrastructurePath: async (serialNumber) => {
    const resp = await api.get(`/api/topology/infrastructure/${encodeURIComponent(serialNumber)}`);
    return resp.data;
  },
  getDevicePath: async (serialNumber) => {
    const resp = await api.get(`/api/topology/device/${encodeURIComponent(serialNumber)}`);
    return resp.data;
  },
  getHeadendTopology: async (headendId) => {
    const resp = await api.get(`/api/topology/headend/${headendId}`);
    return resp.data;
  },
  getFdhTopology: async (fdhId) => {
    const resp = await api.get(`/api/topology/fdh/${fdhId}`);
    return resp.data;
  }
};

export default topologyService;
