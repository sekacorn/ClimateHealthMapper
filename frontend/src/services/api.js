import axios from 'axios';
import { toast } from 'react-hot-toast';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - Handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Server responded with error status
      const { status, data } = error.response;

      switch (status) {
        case 401:
          toast.error('Authentication required. Please login.');
          localStorage.removeItem('authToken');
          window.location.href = '/login';
          break;
        case 403:
          toast.error('Access denied. You do not have permission.');
          break;
        case 404:
          toast.error('Resource not found.');
          break;
        case 429:
          toast.error('Too many requests. Please try again later.');
          break;
        case 500:
          toast.error('Server error. Please try again later.');
          break;
        default:
          toast.error(data?.message || 'An error occurred.');
      }
    } else if (error.request) {
      // Request made but no response received
      toast.error('Network error. Please check your connection.');
    } else {
      // Something else happened
      toast.error('An unexpected error occurred.');
    }

    return Promise.reject(error);
  }
);

// Authentication APIs
export const authAPI = {
  login: (credentials) => api.post('/api/auth/login', credentials),
  register: (userData) => api.post('/api/auth/register', userData),
  logout: () => api.post('/api/auth/logout'),
  refreshToken: () => api.post('/api/auth/refresh'),
  verifyMFA: (code) => api.post('/api/auth/mfa/verify', { code }),
  setupMFA: () => api.post('/api/auth/mfa/setup'),
  googleOAuth: (token) => api.post('/api/auth/oauth/google', { token }),
  azureOAuth: (token) => api.post('/api/auth/oauth/azure', { token }),
  oktaOAuth: (token) => api.post('/api/auth/oauth/okta', { token }),
  samlAuth: (assertion) => api.post('/api/auth/saml', { assertion }),
};

// User APIs
export const userAPI = {
  getProfile: () => api.get('/api/user/profile'),
  updateProfile: (data) => api.put('/api/user/profile', data),
  updateMBTI: (mbtiType) => api.put('/api/user/mbti', { mbti_type: mbtiType }),
  getPreferences: () => api.get('/api/user/preferences'),
  updatePreferences: (prefs) => api.put('/api/user/preferences', prefs),
};

// Data Upload APIs
export const dataAPI = {
  uploadFile: (file, type) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    return api.post('/api/data/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getDatasets: () => api.get('/api/data/datasets'),
  getDataset: (id) => api.get(`/api/data/datasets/${id}`),
  deleteDataset: (id) => api.delete(`/api/data/datasets/${id}`),
};

// Analysis APIs
export const analysisAPI = {
  analyzeHealthRisk: (data) => api.post('/api/analysis/health-risk', data),
  getAnalysisHistory: () => api.get('/api/analysis/history'),
  getAnalysis: (id) => api.get(`/api/analysis/${id}`),
  deleteAnalysis: (id) => api.delete(`/api/analysis/${id}`),
};

// Climate Data APIs
export const climateAPI = {
  getClimateData: (params) => api.get('/api/climate/data', { params }),
  getClimateProjections: (params) => api.get('/api/climate/projections', { params }),
  getExtremeEvents: (params) => api.get('/api/climate/extreme-events', { params }),
};

// Visualization APIs
export const visualizationAPI = {
  generate3DScene: (data) => api.post('/api/visualization/3d-scene', data),
  getVisualization: (id) => api.get(`/api/visualization/${id}`),
  exportVisualization: (id, format) =>
    api.get(`/api/visualization/${id}/export`, {
      params: { format },
      responseType: 'blob',
    }),
};

// LLM Query APIs
export const llmAPI = {
  query: (message, context) => api.post('/api/llm/query', { message, context }),
  getChatHistory: () => api.get('/api/llm/history'),
  clearChatHistory: () => api.delete('/api/llm/history'),
};

// Collaboration APIs
export const collaborationAPI = {
  createSession: (data) => api.post('/api/collaboration/sessions', data),
  getSessions: () => api.get('/api/collaboration/sessions'),
  getSession: (id) => api.get(`/api/collaboration/sessions/${id}`),
  joinSession: (id) => api.post(`/api/collaboration/sessions/${id}/join`),
  leaveSession: (id) => api.post(`/api/collaboration/sessions/${id}/leave`),
  shareAnnotation: (sessionId, annotation) =>
    api.post(`/api/collaboration/sessions/${sessionId}/annotations`, annotation),
};

// Resource Monitoring APIs
export const monitoringAPI = {
  getResourceUsage: () => api.get('/api/monitoring/resources'),
  getSystemHealth: () => api.get('/api/monitoring/health'),
  getLogs: (params) => api.get('/api/monitoring/logs', { params }),
};

// Troubleshooting APIs
export const troubleshootAPI = {
  runDiagnostics: () => api.post('/api/troubleshoot/diagnostics'),
  getErrorLogs: (params) => api.get('/api/troubleshoot/errors', { params }),
  reportIssue: (issue) => api.post('/api/troubleshoot/report', issue),
};

export default api;
