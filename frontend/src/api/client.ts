import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Attach access token to every request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Token refresh logic — queue requests while refreshing
let isRefreshing = false;
let refreshQueue: ((token: string) => void)[] = [];

const flushQueue = (token: string) => {
  refreshQueue.forEach((cb) => cb(token));
  refreshQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;

    if (error.response?.status === 401 && !original._retried) {
      original._retried = true;
      const refreshToken = localStorage.getItem('refreshToken');

      if (!refreshToken) {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      if (!isRefreshing) {
        isRefreshing = true;
        try {
          const res = await axios.post('/api/auth/refresh', { refreshToken });
          const newToken: string = res.data.data.token;
          const newRefresh: string = res.data.data.refreshToken;
          localStorage.setItem('token', newToken);
          localStorage.setItem('refreshToken', newRefresh);
          flushQueue(newToken);
          isRefreshing = false;
          original.headers.Authorization = `Bearer ${newToken}`;
          return apiClient(original);
        } catch {
          isRefreshing = false;
          refreshQueue = [];
          localStorage.clear();
          window.location.href = '/login';
          return Promise.reject(error);
        }
      }

      return new Promise((resolve) => {
        refreshQueue.push((token) => {
          original.headers.Authorization = `Bearer ${token}`;
          resolve(apiClient(original));
        });
      });
    }

    return Promise.reject(error);
  }
);

export default apiClient;
