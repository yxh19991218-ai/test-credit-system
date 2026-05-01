/** 贷款申请相关 API */
import { request, pageGet } from "./request";

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
  }) => pageGet<LoanApplication>("/api/applications", params),

  getById: (id: number) =>
    request.get<LoanApplication>(`/api/applications/${id}`),

  create: (data: ApplicationRequest) =>
    request.post<LoanApplication>("/api/applications", data),

  submit: (id: number) =>
    request.post<string>(`/api/applications/${id}/submit`),

  review: (id: number, data: ReviewRequest) =>
    request.post<string>(`/api/applications/${id}/review`, data),

  cancel: (id: number, reason: string) =>
    request.post<string>(`/api/applications/${id}/cancel`, null, {
      params: { reason },
    }),
};
