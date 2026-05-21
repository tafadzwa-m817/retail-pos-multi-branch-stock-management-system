import apiClient from './client';
import type { ApiResponse } from '../types';

export interface TaxRate {
  id: number;
  name: string;
  rate: number;
  branchId: number | null;
  active: boolean;
}

export const taxRatesApi = {
  getAll: () =>
    apiClient.get<ApiResponse<TaxRate[]>>('/tax-rates').then((r) => r.data.data),

  getForBranch: (branchId: number) =>
    apiClient.get<ApiResponse<TaxRate[]>>(`/tax-rates/branch/${branchId}`).then((r) => r.data.data),

  create: (name: string, rate: number, branchId?: number) =>
    apiClient.post<ApiResponse<TaxRate>>('/tax-rates', null, { params: { name, rate, branchId } }).then((r) => r.data.data),

  update: (id: number, name: string, rate: number, active: boolean) =>
    apiClient.put<ApiResponse<TaxRate>>(`/tax-rates/${id}`, null, { params: { name, rate, active } }).then((r) => r.data.data),

  delete: (id: number) =>
    apiClient.delete(`/tax-rates/${id}`),
};
