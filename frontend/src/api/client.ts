import axios from 'axios';

// Ingestion service API
export const ingestionApi = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

// Query service API
export const queryApi = axios.create({
  baseURL: import.meta.env.VITE_QUERY_API_URL,
});

// Add JWT token to every request automatically
export const setAuthToken = (token: string) => {
  ingestionApi.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  queryApi.defaults.headers.common['Authorization'] = `Bearer ${token}`;
};

// Document API calls
export const documentApi = {
  upload: (file: File, tenantId: string = 'default') => {
    const formData = new FormData();
    formData.append('file', file);
    return ingestionApi.post('/api/v1/documents', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-Tenant-ID': tenantId,
      },
    });
  },

  getAll: (tenantId: string = 'default') =>
    ingestionApi.get('/api/v1/documents', {
      headers: { 'X-Tenant-ID': tenantId },
    }),

  getStatus: (documentId: string) =>
    ingestionApi.get(`/api/v1/documents/${documentId}/status`),
};

// Query API calls
export const queryApiCalls = {
  query: (question: string, documentIds?: string[]) =>
    queryApi.post('/api/v1/query', {
      question,
      documentIds,
      tenantId: 'default',
    }),
};