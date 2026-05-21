import apiClient from './client';
import type { ApiResponse, CustomerResponse } from '../types';

export interface CustomerRequest {
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  address?: string;
}

export const customersApi = {
  getAll: () =>
    apiClient.get<ApiResponse<CustomerResponse[]>>('/customers').then((r) => r.data.data),

  getById: (id: number) =>
    apiClient.get<ApiResponse<CustomerResponse>>(`/customers/${id}`).then((r) => r.data.data),

  search: (q: string) =>
    apiClient.get<ApiResponse<CustomerResponse[]>>(`/customers/search?q=${encodeURIComponent(q)}`).then((r) => r.data.data),

  create: (data: CustomerRequest) =>
    apiClient.post<ApiResponse<CustomerResponse>>('/customers', data).then((r) => r.data.data),

  update: (id: number, data: CustomerRequest) =>
    apiClient.put<ApiResponse<CustomerResponse>>(`/customers/${id}`, data).then((r) => r.data.data),

  delete: (id: number) =>
    apiClient.delete(`/customers/${id}`),
};
