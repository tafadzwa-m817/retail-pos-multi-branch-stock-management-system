import apiClient from './client';
import type { ApiResponse, PurchaseOrderRequest, PurchaseOrderResponse } from '../types';

export const purchaseOrdersApi = {
  getAll: () =>
    apiClient.get<ApiResponse<PurchaseOrderResponse[]>>('/purchase-orders').then((r) => r.data.data),

  getById: (id: number) =>
    apiClient.get<ApiResponse<PurchaseOrderResponse>>(`/purchase-orders/${id}`).then((r) => r.data.data),

  create: (data: PurchaseOrderRequest) =>
    apiClient.post<ApiResponse<PurchaseOrderResponse>>('/purchase-orders', data).then((r) => r.data.data),

  submit: (id: number) =>
    apiClient.put<ApiResponse<PurchaseOrderResponse>>(`/purchase-orders/${id}/submit`).then((r) => r.data.data),

  receive: (id: number) =>
    apiClient.put<ApiResponse<PurchaseOrderResponse>>(`/purchase-orders/${id}/receive`).then((r) => r.data.data),

  cancel: (id: number) =>
    apiClient.put<ApiResponse<PurchaseOrderResponse>>(`/purchase-orders/${id}/cancel`).then((r) => r.data.data),
};
