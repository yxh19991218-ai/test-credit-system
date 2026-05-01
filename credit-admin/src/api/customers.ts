/** 客户相关 API */
import { request, pageGet } from "./request";

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

export const customerApi = {
  list: (params?: {
    name?: string;
    phone?: string;
    keyword?: string;
    status?: string;
    page?: number;
    size?: number;
  }) => pageGet<Customer>("/api/customers", params),

  getById: (id: number) =>
    request.get<Customer>(`/api/customers/${id}`),

  create: (data: CustomerRequest) =>
    request.post<Customer>("/api/customers", data),

  update: (id: number, data: CustomerRequest) =>
    request.put<Customer>(`/api/customers/${id}`, data),

  updateStatus: (id: number, status: string, reason: string) =>
    request.patch<void>(`/api/customers/${id}/status`, { status, reason }),

  delete: (id: number, reason: string) =>
    request.delete<void>(`/api/customers/${id}`, { reason }),
};
