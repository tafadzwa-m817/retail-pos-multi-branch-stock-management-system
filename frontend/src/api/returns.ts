import apiClient from './client';
import type { ApiResponse, ReturnResponse } from '../types';

export interface ReturnRequest {
  originalSaleId: number;
  reason: string;
  items: { saleItemId: number; quantity: number }[];
}

export const returnsApi = {
  getAll: () =>
    apiClient.get<ApiResponse<ReturnResponse[]>>('/returns').then((r) => r.data.data),

  getById: (id: number) =>
    apiClient.get<ApiResponse<ReturnResponse>>(`/returns/${id}`).then((r) => r.data.data),

  getBySale: (saleId: number) =>
    apiClient.get<ApiResponse<ReturnResponse[]>>(`/returns/sale/${saleId}`).then((r) => r.data.data),

  getByBranch: (branchId: number) =>
    apiClient.get<ApiResponse<ReturnResponse[]>>(`/returns/branch/${branchId}`).then((r) => r.data.data),

  processReturn: (data: ReturnRequest) =>
    apiClient.post<ApiResponse<ReturnResponse>>('/returns', data).then((r) => r.data.data),
};
