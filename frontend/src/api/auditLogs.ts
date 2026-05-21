import apiClient from './client';
import type { ApiResponse, AuditLogEntry, PageResponse } from '../types';

export const auditLogsApi = {
  getAll: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<PageResponse<AuditLogEntry>>>(`/audit-logs?page=${page}&size=${size}`).then((r) => r.data.data),

  getByEntityType: (type: string, page = 0, size = 20) =>
    apiClient.get<ApiResponse<PageResponse<AuditLogEntry>>>(`/audit-logs/entity/${type}?page=${page}&size=${size}`).then((r) => r.data.data),

  getByUser: (email: string, page = 0, size = 20) =>
    apiClient.get<ApiResponse<PageResponse<AuditLogEntry>>>(`/audit-logs/user/${encodeURIComponent(email)}?page=${page}&size=${size}`).then((r) => r.data.data),

  getByDateRange: (start: string, end: string, page = 0, size = 20) =>
    apiClient.get<ApiResponse<PageResponse<AuditLogEntry>>>(`/audit-logs/date-range?start=${start}&end=${end}&page=${page}&size=${size}`).then((r) => r.data.data),
};
