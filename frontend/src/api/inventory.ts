import apiClient from './client';
import type { ApiResponse, InventoryAdjustRequest, InventoryResponse } from '../types';

export const inventoryApi = {
  getAll: () =>
    apiClient.get<ApiResponse<InventoryResponse[]>>('/inventory').then((r) => r.data.data),

  getByBranch: (branchId: number) =>
    apiClient.get<ApiResponse<InventoryResponse[]>>(`/inventory/branch/${branchId}`).then((r) => r.data.data),

  getLowStock: (branchId?: number) => {
    const url = branchId ? `/inventory/low-stock?branchId=${branchId}` : '/inventory/low-stock';
    return apiClient.get<ApiResponse<InventoryResponse[]>>(url).then((r) => r.data.data);
  },

  adjust: (data: InventoryAdjustRequest) =>
    apiClient.post<ApiResponse<InventoryResponse>>('/inventory/adjust', data).then((r) => r.data.data),
};
