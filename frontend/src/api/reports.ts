import apiClient from './client';
import type { ApiResponse, SalesReportResponse } from '../types';

export interface DailySummary {
  date: string;
  salesCount: number;
  revenue: number;
}

const downloadBlob = (data: Blob, filename: string) => {
  const url = window.URL.createObjectURL(new Blob([data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const reportsApi = {
  getSalesReport: (params: { branchId?: number; start: string; end: string }) =>
    apiClient.get<ApiResponse<SalesReportResponse>>('/reports/sales', { params }).then((r) => r.data.data),

  getDailySummary: (days = 7) =>
    apiClient.get<ApiResponse<DailySummary[]>>('/reports/daily-summary', { params: { days } }).then((r) => r.data.data),

  exportSalesCSV: async (params: { branchId?: number; start: string; end: string }) => {
    const response = await apiClient.get('/reports/sales/export', { params, responseType: 'blob' });
    downloadBlob(response.data as Blob, 'sales-report.csv');
  },

  exportInventoryCSV: async (branchId: number) => {
    const response = await apiClient.get('/reports/inventory/export', { params: { branchId }, responseType: 'blob' });
    downloadBlob(response.data as Blob, 'inventory-report.csv');
  },

  downloadSalesPdf: async (params: { branchId?: number; start: string; end: string }) => {
    const response = await apiClient.get('/reports/sales/pdf', { params, responseType: 'blob' });
    downloadBlob(response.data as Blob, 'sales-report.pdf');
  },

  downloadInventoryPdf: async (branchId: number) => {
    const response = await apiClient.get(`/reports/inventory/${branchId}/pdf`, { responseType: 'blob' });
    downloadBlob(response.data as Blob, 'inventory-report.pdf');
  },

  downloadSalesExcel: async (params: { branchId?: number; start: string; end: string }) => {
    const response = await apiClient.get('/reports/sales/excel', { params, responseType: 'blob' });
    downloadBlob(response.data as Blob, 'sales-report.xlsx');
  },

  downloadInventoryExcel: async (branchId: number) => {
    const response = await apiClient.get(`/reports/inventory/${branchId}/excel`, { responseType: 'blob' });
    downloadBlob(response.data as Blob, 'inventory-report.xlsx');
  },
};
