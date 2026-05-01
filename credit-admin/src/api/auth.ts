/** 认证相关 API */
import { request } from "./request";

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
    request.post<LoginResponse>("/api/auth/login", data),

  refresh: (data: RefreshRequest) =>
    request.post<LoginResponse>("/api/auth/refresh", data),
};
