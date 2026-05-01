/** 产品相关 API */
import { request } from "./request";

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
    request.get<Product[]>("/api/products"),

  getById: (id: number) =>
    request.get<Product>(`/api/products/${id}`),

  create: (data: ProductRequest) =>
    request.post<Product>("/api/products", data),

  update: (id: number, data: Partial<ProductRequest>) =>
    request.put<Product>(`/api/products/${id}`, data),

  publish: (id: number) =>
    request.post<string>(`/api/products/${id}/publish`, null, {
      params: { operator: "ADMIN" },
    }),

  unpublish: (id: number) =>
    request.post<string>(`/api/products/${id}/unpublish`, null, {
      params: { reason: "manual unpublish", operator: "ADMIN" },
    }),
};
