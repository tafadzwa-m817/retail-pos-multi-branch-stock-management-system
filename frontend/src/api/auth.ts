import axios from 'axios';
import type { ApiResponse, AuthResponse, LoginRequest, RefreshTokenRequest } from '../types';

export const authApi = {
  login: (data: LoginRequest) =>
    axios.post<ApiResponse<AuthResponse>>('/api/auth/login', data).then((r) => r.data.data),

  logout: (data: RefreshTokenRequest) =>
    axios.post('/api/auth/logout', data),
};
