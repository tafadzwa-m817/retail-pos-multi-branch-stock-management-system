import apiClient from './client';
import type { ApiResponse, ProductResponse } from '../types';

export const productsApi = {
  getAll: () =>
    apiClient.get<ApiResponse<ProductResponse[]>>('/products').then((r) => r.data.data),

  getAllAdmin: () =>
    apiClient.get<ApiResponse<ProductResponse[]>>('/products/all').then((r) => r.data.data),

  search: (q: string) =>
    apiClient.get<ApiResponse<ProductResponse[]>>(`/products/search?q=${encodeURIComponent(q)}`).then((r) => r.data.data),

  getById: (id: number) =>
    apiClient.get<ApiResponse<ProductResponse>>(`/products/${id}`).then((r) => r.data.data),

  getByBarcode: (barcode: string) =>
    apiClient.get<ApiResponse<ProductResponse>>(`/products/barcode/${encodeURIComponent(barcode)}`).then((r) => r.data.data),
};
