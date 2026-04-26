/** 产品相关 API */
import apiClient from "./client";

export interface Product {
  id: number;
  productCode: string;
  productName: string;
  productDescription: string;
  status: string;
  interestRate: number;
  minAmount: number;
  maxAmount: number;
  minTerm: number;
  maxTerm: number;
  createdAt: string;
}

export interface ProductRequest {
  productCode: string;
  productName: string;
  productDescription?: string;
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
    active
      ? apiClient.post(`/api/products/${id}/publish`, null, {
          params: { operator: "ADMIN" },
        })
      : apiClient.post(`/api/products/${id}/unpublish`, null, {
          params: { reason: "manual unpublish", operator: "ADMIN" },
        }),

  /** 兼容后端返回字段：将 LoanProductResponse 转为前端 Product */
  transformResponse: (data: any): Product => ({
    id: data.id,
    productCode: data.productCode,
    productName: data.productName,
    productDescription: data.productDescription ?? "",
    status: data.status,
    interestRate: data.interestRate,
    minAmount: data.minAmount,
    maxAmount: data.maxAmount,
    minTerm: data.minTerm,
    maxTerm: data.maxTerm,
    createdAt: data.createdAt,
  }),

  transformListResponse: (data: any[]): Product[] =>
    (data ?? []).map(productApi.transformResponse),
};
