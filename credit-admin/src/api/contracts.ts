/** 合同相关 API */
import { request, pageGet } from "./request";

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
  }) => pageGet<Contract>("/api/contracts", params),

  getById: (id: number) =>
    request.get<Contract>(`/api/contracts/${id}`),

  create: (data: ContractRequest) =>
    request.post<Contract>("/api/contracts", data),

  sign: (id: number, signatory: string, signatureMethod = "ONLINE") =>
    request.post<string>(`/api/contracts/${id}/sign`, null, {
      params: { signatory, signatureMethod },
    }),

  terminate: (id: number, reason: string) =>
    request.post<string>(`/api/contracts/${id}/terminate`, null, {
      params: { reason },
    }),

  extend: (id: number, months: number, reason: string) =>
    request.post<string>(`/api/contracts/${id}/extend`, null, {
      params: { months, reason },
    }),

  settle: (id: number) =>
    request.post<string>(`/api/contracts/${id}/settle`),

  getOverdue: () =>
    request.get<Contract[]>("/api/contracts/overdue"),
};
