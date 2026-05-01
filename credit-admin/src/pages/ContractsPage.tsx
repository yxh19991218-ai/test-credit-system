/** 合同管理页面 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { contractApi, type Contract } from "../api/contracts";

const statusColors: Record<string, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-700",
  COMPLETED: "bg-blue-100 text-blue-700",
  OVERDUE: "bg-red-100 text-red-700",
  TERMINATED: "bg-slate-100 text-slate-600",
  PENDING_SIGN: "bg-amber-100 text-amber-700",
};

const statusLabels: Record<string, string> = {
  ACTIVE: "执行中",
  COMPLETED: "已结清",
  OVERDUE: "已逾期",
  TERMINATED: "已终止",
  PENDING_SIGN: "待签署",
};

export default function ContractsPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["contracts", statusFilter],
    queryFn: () =>
      contractApi.list({
        status: statusFilter,
        page: 0,
        size: 100,
      }),
  });

  const signMutation = useMutation({
    mutationFn: (id: number) => contractApi.sign(id, "admin"),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });

  const terminateMutation = useMutation({
    mutationFn: (id: number) => contractApi.terminate(id, ""),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });

  const settleMutation = useMutation({
    mutationFn: (id: number) => contractApi.settle(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });

  const extendMutation = useMutation({
    mutationFn: ({ id, newTerm }: { id: number; newTerm: number }) =>
      contractApi.extend(id, newTerm, ""),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });

  const handleExtend = (contract: Contract) => {
    const input = prompt("请输入展期月数：", "12");
    if (input) {
      const newTerm = parseInt(input, 10);
      if (!isNaN(newTerm) && newTerm > 0) {
        extendMutation.mutate({ id: contract.id, newTerm });
      }
    }
  };

  const contracts = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">合同管理</h1>
        <p className="text-slate-500 mt-1">查看和管理所有贷款合同</p>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex gap-3 flex-wrap">
        {[
          "",
          "PENDING_SIGN",
          "ACTIVE",
          "COMPLETED",
          "OVERDUE",
          "TERMINATED",
        ].map((s) => (
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
                合同编号
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                客户
              </th>
              <th className="text-right px-6 py-3 text-sm font-semibold text-slate-600">
                贷款金额
              </th>
              <th className="text-right px-6 py-3 text-sm font-semibold text-slate-600">
                剩余本金
              </th>
              <th className="text-right px-6 py-3 text-sm font-semibold text-slate-600">
                期限(月)
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                状态
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                创建时间
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
            ) : contracts.length === 0 ? (
              <tr>
                <td colSpan={8} className="text-center py-12 text-slate-400">
                  暂无合同
                </td>
              </tr>
            ) : (
              contracts.map((contract: Contract) => (
                <tr
                  key={contract.id}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >
                  <td className="px-6 py-4 text-sm font-mono text-slate-800">
                    {contract.contractNo}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {`#${contract.customerId}`}
                  </td>
                  <td className="px-6 py-4 text-sm text-right font-medium">
                    ¥{contract.totalAmount.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 text-sm text-right">
                    ¥{(contract.remainingPrincipal ?? 0).toLocaleString()}
                  </td>
                  <td className="px-6 py-4 text-sm text-right text-slate-600">
                    {contract.term}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`text-xs px-2 py-1 rounded-full ${
                        statusColors[contract.status] ??
                        "bg-slate-100 text-slate-600"
                      }`}
                    >
                      {statusLabels[contract.status] ?? contract.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-500">
                    {contract.createdAt
                      ? new Date(contract.createdAt).toLocaleDateString("zh-CN")
                      : "-"}
                  </td>
                  <td className="px-6 py-4 text-right space-x-2">
                    {contract.status === "PENDING_SIGN" && (
                      <button
                        onClick={() => signMutation.mutate(contract.id)}
                        className="text-emerald-600 hover:text-emerald-800 text-sm font-medium"
                      >
                        签署
                      </button>
                    )}
                    {contract.status === "ACTIVE" && (
                      <>
                        <button
                          onClick={() => handleExtend(contract)}
                          className="text-blue-600 hover:text-blue-800 text-sm"
                        >
                          展期
                        </button>
                        <button
                          onClick={() => {
                            if (confirm("确定终止该合同？"))
                              terminateMutation.mutate(contract.id);
                          }}
                          className="text-red-600 hover:text-red-800 text-sm"
                        >
                          终止
                        </button>
                        <button
                          onClick={() => settleMutation.mutate(contract.id)}
                          className="text-emerald-600 hover:text-emerald-800 text-sm"
                        >
                          结清
                        </button>
                      </>
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
