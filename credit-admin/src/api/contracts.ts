/** 合同相关 API */
import apiClient from "./client";

export interface Contract {
  id: number;
  applicationId: number;
  contractNo: string;
  customerId: number;
  productId: number;
  totalAmount: number;
  remainingPrincipal: number;
  term: number;
  paidPeriods: number;
  interestRate: number;
  repaymentMethod: string;
  startDate: string;
  endDate: string;
  status: string;
  signDate: string;
  signatory: string;
  terminationReason: string;
  extendedMonths: number;
  createdAt: string;
}

export interface ContractRequest {
  applicationId?: number;
  customerId: number;
  productId: number;
  totalAmount: number;
  term: number;
  interestRate: number;
  repaymentMethod: string;
  startDate: string;
  endDate: string;
}

export const contractApi = {
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
        content: Contract[];
        totalElements: number;
        totalPages: number;
        number: number;
        size: number;
      };
    }>("/api/contracts", { params }),

  getById: (id: number) =>
    apiClient.get<{ code: number; message: string; data: Contract }>(
      `/api/contracts/${id}`,
    ),

  create: (data: ContractRequest) =>
    apiClient.post<{ code: number; message: string; data: Contract }>(
      "/api/contracts",
      data,
    ),

  sign: (id: number, signatory: string, signatureMethod?: string) =>
    apiClient.post(`/api/contracts/${id}/sign`, null, {
      params: { signatory, signatureMethod: signatureMethod || "ONLINE" },
    }),

  terminate: (id: number, reason: string) =>
    apiClient.post(`/api/contracts/${id}/terminate`, null, {
      params: { reason },
    }),

  extend: (id: number, months: number, reason: string) =>
    apiClient.post(`/api/contracts/${id}/extend`, null, {
      params: { months, reason },
    }),

  settle: (id: number) => apiClient.post(`/api/contracts/${id}/settle`),

  getOverdue: () =>
    apiClient.get<{ code: number; message: string; data: Contract[] }>(
      "/api/contracts/overdue",
    ),
};
