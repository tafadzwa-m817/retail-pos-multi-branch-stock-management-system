import apiClient from './client';
import type { ApiResponse, StockTransferRequest, StockTransferResponse } from '../types';

export const stockTransfersApi = {
  getAll: () =>
    apiClient.get<ApiResponse<StockTransferResponse[]>>('/stock-transfers').then((r) => r.data.data),

  getById: (id: number) =>
    apiClient.get<ApiResponse<StockTransferResponse>>(`/stock-transfers/${id}`).then((r) => r.data.data),

  create: (data: StockTransferRequest) =>
    apiClient.post<ApiResponse<StockTransferResponse>>('/stock-transfers', data).then((r) => r.data.data),

  approve: (id: number) =>
    apiClient.put<ApiResponse<StockTransferResponse>>(`/stock-transfers/${id}/approve`).then((r) => r.data.data),

  complete: (id: number) =>
    apiClient.put<ApiResponse<StockTransferResponse>>(`/stock-transfers/${id}/complete`).then((r) => r.data.data),

  cancel: (id: number) =>
    apiClient.put<ApiResponse<StockTransferResponse>>(`/stock-transfers/${id}/cancel`).then((r) => r.data.data),
};
