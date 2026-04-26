/** 贷款申请相关 API */
import apiClient from "./client";

export interface LoanApplication {
  id: number;
  customerId: number;
  productId: number;
  applyAmount: number;
  applyTerm: number;
  purpose: string;
  status: string;
  approvedAmount: number;
  approvedTerm: number;
  interestRate: number;
  monthlyPayment: number;
  applicationDate: string;
  submitDate: string;
  reviewer: string;
  reviewComments: string;
  contractId: number;
  createdAt: string;
}

export interface ApplicationRequest {
  customerId: number;
  productId: number;
  applyAmount: number;
  applyTerm: number;
  purpose?: string;
}

export interface ReviewRequest {
  decision: string;
  reviewer: string;
  comments?: string;
  approvedAmount?: number;
  approvedTerm?: number;
  interestRate?: number;
}

export const applicationApi = {
  list: (params?: {
    customerId?: number;
    status?: string;
    page?: number;
    size?: number;
  }) =>
    apiClient.get<{
      code: number;
      message: string;
      data: {
        content: LoanApplication[];
        totalElements: number;
        totalPages: number;
        number: number;
        size: number;
      };
    }>("/api/applications", { params }),

  getById: (id: number) =>
    apiClient.get<{ code: number; message: string; data: LoanApplication }>(
      `/api/applications/${id}`,
    ),

  create: (data: ApplicationRequest) =>
    apiClient.post<{ code: number; message: string; data: LoanApplication }>(
      "/api/applications",
      data,
    ),

  submit: (id: number) => apiClient.post(`/api/applications/${id}/submit`),

  review: (id: number, data: ReviewRequest) =>
    apiClient.post(`/api/applications/${id}/review`, data),

  cancel: (id: number, reason: string) =>
    apiClient.post(`/api/applications/${id}/cancel`, null, {
      params: { reason },
    }),
};
