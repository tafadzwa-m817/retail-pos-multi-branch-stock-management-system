import apiClient from './client';
import type { ApiResponse, BranchResponse } from '../types';

export const branchesApi = {
  getAll: () =>
    apiClient.get<ApiResponse<BranchResponse[]>>('/branches').then((r) => r.data.data),

  getActive: () =>
    apiClient.get<ApiResponse<BranchResponse[]>>('/branches/active').then((r) => r.data.data),
};
