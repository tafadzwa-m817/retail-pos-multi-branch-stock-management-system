import apiClient from './client';
import type { ApiResponse, PromotionResponse } from '../types';

export interface PromotionRequest {
  name: string;
  description?: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  startDate: string;
  endDate: string;
  minimumPurchaseAmount?: number;
  applyToAll: boolean;
  productIds?: number[];
  active: boolean;
}

export const promotionsApi = {
  getAll: () =>
    apiClient.get<ApiResponse<PromotionResponse[]>>('/promotions').then((r) => r.data.data),

  getActive: () =>
    apiClient.get<ApiResponse<PromotionResponse[]>>('/promotions/active').then((r) => r.data.data),

  create: (data: PromotionRequest) =>
    apiClient.post<ApiResponse<PromotionResponse>>('/promotions', data).then((r) => r.data.data),

  update: (id: number, data: PromotionRequest) =>
    apiClient.put<ApiResponse<PromotionResponse>>(`/promotions/${id}`, data).then((r) => r.data.data),

  delete: (id: number) =>
    apiClient.delete(`/promotions/${id}`),
};
