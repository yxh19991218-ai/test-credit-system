/** 客户相关 API */
import apiClient from "./client";

export interface Customer {
  id: number;
  name: string;
  idCard: string;
  phone: string;
  email: string;
  occupation: string;
  monthlyIncome: number;
  address: string;
  creditScore: number;
  status: string;
  riskLevel: string;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerRequest {
  name: string;
  idCard: string;
  phone: string;
  email?: string;
  occupation?: string;
  monthlyIncome?: number;
  address?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const customerApi = {
  list: (params?: {
    name?: string;
    phone?: string;
    keyword?: string;
    status?: string;
    page?: number;
    size?: number;
  }) =>
    apiClient.get<{
      code: number;
      message: string;
      data: PageResponse<Customer>;
    }>("/api/customers", { params }),

  getById: (id: number) =>
    apiClient.get<{ code: number; message: string; data: Customer }>(
      `/api/customers/${id}`,
    ),

  create: (data: CustomerRequest) =>
    apiClient.post<{ code: number; message: string; data: Customer }>(
      "/api/customers",
      data,
    ),

  update: (id: number, data: CustomerRequest) =>
    apiClient.put<{ code: number; message: string; data: Customer }>(
      `/api/customers/${id}`,
      data,
    ),

  updateStatus: (id: number, status: string, reason: string) =>
    apiClient.patch(`/api/customers/${id}/status`, { status, reason }),

  delete: (id: number, reason: string) =>
    apiClient.delete(`/api/customers/${id}`, { params: { reason } }),
};
