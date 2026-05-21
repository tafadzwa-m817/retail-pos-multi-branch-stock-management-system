import apiClient from './client';
import type { ApiResponse, CategoryResponse } from '../types';

export interface CategoryRequest {
  name: string;
  description?: string;
}

export const categoriesApi = {
  getAll: () =>
    apiClient.get<ApiResponse<CategoryResponse[]>>('/categories').then((r) => r.data.data),

  create: (data: CategoryRequest) =>
    apiClient.post<ApiResponse<CategoryResponse>>('/categories', data).then((r) => r.data.data),

  update: (id: number, data: CategoryRequest) =>
    apiClient.put<ApiResponse<CategoryResponse>>(`/categories/${id}`, data).then((r) => r.data.data),

  delete: (id: number) =>
    apiClient.delete(`/categories/${id}`),
};
