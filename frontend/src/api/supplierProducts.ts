import apiClient from './client';
import type { ApiResponse } from '../types';

export interface SupplierProduct {
  id: number;
  supplier: { id: number; name: string };
  product: { id: number; name: string; sku: string };
  unitCost: number;
  supplierSku?: string;
  preferred: boolean;
  lastUpdated: string;
}

export const supplierProductsApi = {
  getCatalog: (supplierId: number) =>
    apiClient.get<ApiResponse<SupplierProduct[]>>(`/supplier-products/supplier/${supplierId}`).then((r) => r.data.data),

  getByProduct: (productId: number) =>
    apiClient.get<ApiResponse<SupplierProduct[]>>(`/supplier-products/product/${productId}`).then((r) => r.data.data),

  upsert: (data: { supplierId: number; productId: number; unitCost: number; supplierSku?: string; preferred?: boolean }) =>
    apiClient.post<ApiResponse<SupplierProduct>>('/supplier-products', data).then((r) => r.data.data),

  delete: (id: number) => apiClient.delete(`/supplier-products/${id}`),
};
