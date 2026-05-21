import apiClient from './client';
import type { ApiResponse, PageResponse, SaleRequest, SaleResponse } from '../types';

export const salesApi = {
  getAll: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<PageResponse<SaleResponse>>>(`/sales?page=${page}&size=${size}`).then((r) => r.data.data),

  getById: (id: number) =>
    apiClient.get<ApiResponse<SaleResponse>>(`/sales/${id}`).then((r) => r.data.data),

  getByBranch: (branchId: number) =>
    apiClient.get<ApiResponse<SaleResponse[]>>(`/sales/branch/${branchId}`).then((r) => r.data.data),

  create: (data: SaleRequest) =>
    apiClient.post<ApiResponse<SaleResponse>>('/sales', data).then((r) => r.data.data),

  voidSale: (id: number) =>
    apiClient.put<ApiResponse<SaleResponse>>(`/sales/${id}/void`).then((r) => r.data.data),
};
