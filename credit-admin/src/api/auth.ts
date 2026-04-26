/** 认证相关 API */
import apiClient from "./client";

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  username: string;
  role: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<{ code: number; message: string; data: LoginResponse }>(
      "/api/auth/login",
      data,
    ),

  refresh: (data: RefreshRequest) =>
    apiClient.post<{ code: number; message: string; data: LoginResponse }>(
      "/api/auth/refresh",
      data,
    ),
};
