/** 仪表盘相关 API */
import type { DashboardResponse } from "../types/dashboard";
import { request } from "./request";

export const dashboardApi = {
  getOverview: () =>
    request.get<DashboardResponse>("/api/dashboard/overview"),
};
