import apiClient from './client';
import type { ApiResponse } from '../types';

export interface CreditAccount {
  id: number;
  customer: { id: number; firstName: string; lastName: string; email: string };
  creditLimit: number;
  currentBalance: number;
  availableCredit: number;
  active: boolean;
  createdAt: string;
}

export interface CreditTransaction {
  id: number;
  amount: number;
  type: 'DEBIT' | 'REPAYMENT';
  saleId?: number;
  notes?: string;
  createdAt: string;
}

export const creditAccountsApi = {
  getAll: () =>
    apiClient.get<ApiResponse<CreditAccount[]>>('/credit-accounts').then((r) => r.data.data),

  getById: (id: number) =>
    apiClient.get<ApiResponse<CreditAccount>>(`/credit-accounts/${id}`).then((r) => r.data.data),

  getByCustomer: (customerId: number) =>
    apiClient.get<ApiResponse<CreditAccount>>(`/credit-accounts/customer/${customerId}`).then((r) => r.data.data),

  getTransactions: (id: number) =>
    apiClient.get<ApiResponse<CreditTransaction[]>>(`/credit-accounts/${id}/transactions`).then((r) => r.data.data),

  openAccount: (customerId: number, creditLimit: number) =>
    apiClient.post<ApiResponse<CreditAccount>>('/credit-accounts', { customerId, creditLimit }).then((r) => r.data.data),

  updateLimit: (id: number, creditLimit: number) =>
    apiClient.put<ApiResponse<CreditAccount>>(`/credit-accounts/${id}/limit`, { creditLimit }).then((r) => r.data.data),

  recordRepayment: (id: number, amount: number, notes?: string) =>
    apiClient.post<ApiResponse<CreditTransaction>>(`/credit-accounts/${id}/repayment`, { amount, notes }).then((r) => r.data.data),

  close: (id: number) =>
    apiClient.put(`/credit-accounts/${id}/close`),
};
