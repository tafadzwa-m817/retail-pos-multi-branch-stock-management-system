import apiClient from './client';
import type { ApiResponse } from '../types';

export interface ExpenseCategory {
  id: number;
  name: string;
  color: string;
}

export interface Expense {
  id: number;
  branch: { id: number; name: string };
  category: ExpenseCategory;
  description: string;
  amount: number;
  expenseDate: string;
  recordedBy: { id: number; firstName: string; lastName: string };
  notes?: string;
  createdAt: string;
}

export const expensesApi = {
  getCategories: () =>
    apiClient.get<ApiResponse<ExpenseCategory[]>>('/expenses/categories').then((r) => r.data.data),

  getAll: () =>
    apiClient.get<ApiResponse<Expense[]>>('/expenses').then((r) => r.data.data),

  getByBranch: (branchId: number) =>
    apiClient.get<ApiResponse<Expense[]>>(`/expenses/branch/${branchId}`).then((r) => r.data.data),

  create: (data: {
    branchId: number; categoryId: number; description: string;
    amount: number; expenseDate: string; notes?: string;
  }) => apiClient.post<ApiResponse<Expense>>('/expenses', data).then((r) => r.data.data),

  delete: (id: number) => apiClient.delete(`/expenses/${id}`),
};
