/** 产品相关 API */
import apiClient from "./client";

export interface Product {
  id: number;
  name: string;
  description: string;
  minAmount: number;
  maxAmount: number;
  minTerm: number;
  maxTerm: number;
  interestRate: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  name: string;
  description?: string;
  minAmount: number;
  maxAmount: number;
  minTerm: number;
  maxTerm: number;
  interestRate: number;
}

export const productApi = {
  list: () =>
    apiClient.get<{ code: number; message: string; data: Product[] }>(
      "/api/products",
    ),

  getById: (id: number) =>
    apiClient.get<{ code: number; message: string; data: Product }>(
      `/api/products/${id}`,
    ),

  create: (data: ProductRequest) =>
    apiClient.post<{ code: number; message: string; data: Product }>(
      "/api/products",
      data,
    ),

  update: (id: number, data: Partial<ProductRequest>) =>
    apiClient.put<{ code: number; message: string; data: Product }>(
      `/api/products/${id}`,
      data,
    ),

  toggleActive: (id: number, active: boolean) =>
    apiClient.patch(`/api/products/${id}/status`, null, {
      params: { active },
    }),
};
