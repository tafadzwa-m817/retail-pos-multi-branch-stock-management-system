import apiClient from './client';
import axios from 'axios';
import type { ApiResponse } from '../types';

export interface StoreSettings {
  id: number;
  storeName: string;
  storePhone: string;
  storeAddress: string;
  logoUrl?: string;
  receiptFooterText: string;
  currency: string;
}

export interface StoreSettingsRequest {
  storeName?: string;
  storePhone?: string;
  storeAddress?: string;
  logoUrl?: string;
  receiptFooterText?: string;
  currency?: string;
}

export const storeSettingsApi = {
  // GET is public — no auth needed
  get: () =>
    axios.get<ApiResponse<StoreSettings>>('/api/store-settings').then((r) => r.data.data),

  update: (data: StoreSettingsRequest) =>
    apiClient.put<ApiResponse<StoreSettings>>('/store-settings', data).then((r) => r.data.data),
};
