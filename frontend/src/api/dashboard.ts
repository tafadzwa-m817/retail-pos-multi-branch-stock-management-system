import apiClient from './client';
import type { ApiResponse, DashboardResponse } from '../types';

export const dashboardApi = {
  get: () =>
    apiClient.get<ApiResponse<DashboardResponse>>('/dashboard').then((r) => r.data.data),
};
