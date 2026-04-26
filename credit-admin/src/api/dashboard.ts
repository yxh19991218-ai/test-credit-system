/** 仪表盘相关 API */
import type { DashboardResponse } from "../types/dashboard";
import apiClient from "./client";

export const dashboardApi = {
  getOverview: () =>
    apiClient.get<{ code: number; message: string; data: DashboardResponse }>(
      "/api/dashboard/overview",
    ),
};
