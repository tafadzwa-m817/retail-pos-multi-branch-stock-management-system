import apiClient from './client';
import type { ApiResponse } from '../types';

export type WastageReason = 'DAMAGED' | 'EXPIRED' | 'STOLEN' | 'QUALITY_ISSUE' | 'OTHER';

export interface WastageRecord {
  id: number;
  branch: { id: number; name: string };
  product: { id: number; name: string; sku: string };
  quantity: number;
  reason: WastageReason;
  notes?: string;
  recordedBy: { id: number; firstName: string; lastName: string };
  wastedAt: string;
  createdAt: string;
}

export const wastageApi = {
  getAll: () =>
    apiClient.get<ApiResponse<WastageRecord[]>>('/wastage').then((r) => r.data.data),

  getByBranch: (branchId: number) =>
    apiClient.get<ApiResponse<WastageRecord[]>>(`/wastage/branch/${branchId}`).then((r) => r.data.data),

  record: (data: {
    branchId: number; productId: number; quantity: number;
    reason: WastageReason; notes?: string; wastedAt?: string;
  }) => apiClient.post<ApiResponse<WastageRecord>>('/wastage', data).then((r) => r.data.data),

  delete: (id: number) => apiClient.delete(`/wastage/${id}`),
};
