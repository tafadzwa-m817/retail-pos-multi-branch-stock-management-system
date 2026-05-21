import apiClient from './client';
import type { ApiResponse, CashShiftResponse } from '../types';

export const cashShiftsApi = {
  getByBranch: (branchId: number) =>
    apiClient.get<ApiResponse<CashShiftResponse[]>>(`/shifts/branch/${branchId}`).then((r) => r.data.data),

  getById: (id: number) =>
    apiClient.get<ApiResponse<CashShiftResponse>>(`/shifts/${id}`).then((r) => r.data.data),

  getCurrentShift: (branchId: number) =>
    apiClient.get<ApiResponse<CashShiftResponse>>(`/shifts/branch/${branchId}/current`).then((r) => r.data.data),

  openShift: (data: { branchId: number; openingFloat: number; notes?: string }) =>
    apiClient.post<ApiResponse<CashShiftResponse>>('/shifts/open', data).then((r) => r.data.data),

  closeShift: (id: number, data: { closingCash: number; notes?: string }) =>
    apiClient.put<ApiResponse<CashShiftResponse>>(`/shifts/${id}/close`, data).then((r) => r.data.data),
};
