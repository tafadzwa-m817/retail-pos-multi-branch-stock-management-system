import apiClient from './client';
import type { ApiResponse } from '../types';

export interface SalesTarget {
  id: number;
  branch: { id: number; name: string };
  targetAmount: number;
  month: number;
  year: number;
  createdAt: string;
}

export interface TargetProgress {
  branchId: number;
  branchName: string;
  target: number;
  achieved: number;
  progressPct: number;
  month: number;
  year: number;
}

export const salesTargetsApi = {
  getAll: () =>
    apiClient.get<ApiResponse<SalesTarget[]>>('/sales-targets').then((r) => r.data.data),

  getProgress: () =>
    apiClient.get<ApiResponse<TargetProgress[]>>('/sales-targets/progress').then((r) => r.data.data),

  setTarget: (data: { branchId: number; targetAmount: number; month: number; year: number }) =>
    apiClient.post<ApiResponse<SalesTarget>>('/sales-targets', data).then((r) => r.data.data),

  delete: (id: number) => apiClient.delete(`/sales-targets/${id}`),
};
