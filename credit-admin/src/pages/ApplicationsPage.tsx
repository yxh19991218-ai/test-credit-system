/** 贷款申请管理页面 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { applicationApi, type LoanApplication } from "../api/applications";

const statusColors: Record<string, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  APPROVED: "bg-emerald-100 text-emerald-700",
  REJECTED: "bg-red-100 text-red-700",
  CANCELLED: "bg-slate-100 text-slate-600",
};

const statusLabels: Record<string, string> = {
  PENDING: "待审批",
  APPROVED: "已通过",
  REJECTED: "已驳回",
  CANCELLED: "已取消",
};

export default function ApplicationsPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["applications", statusFilter],
    queryFn: () =>
      applicationApi.list({
        status: statusFilter,
        page: 0,
        size: 100,
      }),
  });

  const approveMutation = useMutation({
    mutationFn: (id: number) =>
      applicationApi.review(id, { decision: "APPROVE", reviewer: "admin" }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["applications"] }),
  });

  const rejectMutation = useMutation({
    mutationFn: (id: number) =>
      applicationApi.review(id, {
        decision: "REJECT",
        reviewer: "admin",
        comments: "驳回",
      }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["applications"] }),
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => applicationApi.cancel(id, ""),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["applications"] }),
  });

  const applications = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">贷款申请</h1>
        <p className="text-slate-500 mt-1">审核与管理贷款申请</p>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex gap-3">
        {["", "PENDING", "APPROVED", "REJECTED", "CANCELLED"].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              statusFilter === s
                ? "bg-emerald-600 text-white"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            }`}
          >
            {s === "" ? "全部" : (statusLabels[s] ?? s)}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                ID
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                客户
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                产品
              </th>
              <th className="text-right px-6 py-3 text-sm font-semibold text-slate-600">
                金额
              </th>
              <th className="text-right px-6 py-3 text-sm font-semibold text-slate-600">
                期限(月)
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                状态
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                申请时间
              </th>
              <th className="text-right px-6 py-3 text-sm font-semibold text-slate-600">
                操作
              </th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={8} className="text-center py-12 text-slate-400">
                  加载中...
                </td>
              </tr>
            ) : applications.length === 0 ? (
              <tr>
                <td colSpan={8} className="text-center py-12 text-slate-400">
                  暂无申请
                </td>
              </tr>
            ) : (
              applications.map((app: LoanApplication) => (
                <tr
                  key={app.id}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >
                  <td className="px-6 py-4 text-sm text-slate-600">{app.id}</td>
                  <td className="px-6 py-4 text-sm font-medium text-slate-800">
                    {`#${app.customerId}`}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {`#${app.productId}`}
                  </td>
                  <td className="px-6 py-4 text-sm text-right font-medium">
                    ¥{app.applyAmount.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 text-sm text-right text-slate-600">
                    {app.applyTerm}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`text-xs px-2 py-1 rounded-full ${
                        statusColors[app.status] ??
                        "bg-slate-100 text-slate-600"
                      }`}
                    >
                      {statusLabels[app.status] ?? app.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-500">
                    {app.applicationDate
                      ? new Date(app.applicationDate).toLocaleDateString(
                          "zh-CN",
                        )
                      : "-"}
                  </td>
                  <td className="px-6 py-4 text-right space-x-2">
                    {app.status === "PENDING" && (
                      <>
                        <button
                          onClick={() => approveMutation.mutate(app.id)}
                          className="text-emerald-600 hover:text-emerald-800 text-sm font-medium"
                        >
                          通过
                        </button>
                        <button
                          onClick={() => {
                            const remark = prompt("请输入驳回原因：");
                            if (remark !== null) rejectMutation.mutate(app.id);
                          }}
                          className="text-red-600 hover:text-red-800 text-sm font-medium"
                        >
                          驳回
                        </button>
                      </>
                    )}
                    {app.status === "PENDING" && (
                      <button
                        onClick={() => cancelMutation.mutate(app.id)}
                        className="text-slate-500 hover:text-slate-700 text-sm"
                      >
                        取消
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
