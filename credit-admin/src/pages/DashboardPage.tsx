/** 仪表盘页面 */
import { useQuery } from "@tanstack/react-query";
import {
    Bar,
    BarChart,
    CartesianGrid,
    Cell,
    Pie,
    PieChart,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import { dashboardApi } from "../api/dashboard";

const PIE_COLORS = ["#10b981", "#3b82f6", "#f59e0b", "#ef4444", "#8b5cf6"];

function StatCard({
  title,
  value,
  subtitle,
  color,
}: {
  title: string;
  value: string | number;
  subtitle?: string;
  color: string;
}) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
      <p className="text-sm text-slate-500 mb-1">{title}</p>
      <p className={`text-3xl font-bold ${color}`}>{value}</p>
      {subtitle && <p className="text-xs text-slate-400 mt-1">{subtitle}</p>}
    </div>
  );
}

export default function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["dashboard"],
    queryFn: () => dashboardApi.getOverview(),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
      </div>
    );
  }

  const overview = data?.overview;
  const statusData = data?.contractStatusDistribution
    ? Object.entries(data.contractStatusDistribution).map(([name, value]) => ({
        name,
        value,
      }))
    : [];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-800">仪表盘</h1>
        <p className="text-slate-500 mt-1">系统概览与关键指标</p>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="客户总数"
          value={overview?.totalCustomers ?? 0}
          color="text-emerald-600"
        />
        <StatCard
          title="活跃合同"
          value={overview?.activeContracts ?? 0}
          color="text-blue-600"
        />
        <StatCard
          title="待审批申请"
          value={overview?.pendingApplications ?? 0}
          color="text-amber-600"
        />
        <StatCard
          title="逾期合同"
          value={overview?.overdueContracts ?? 0}
          color="text-red-600"
          subtitle={`逾期金额: ¥${(overview?.totalOverdueAmount ?? 0).toLocaleString()}`}
        />
      </div>

      {/* Second Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard
          title="贷款总额"
          value={`¥${(overview?.totalLoanAmount ?? 0).toLocaleString()}`}
          color="text-slate-800"
        />
        <StatCard
          title="剩余本金"
          value={`¥${(overview?.totalRemainingPrincipal ?? 0).toLocaleString()}`}
          color="text-slate-800"
        />
        <StatCard
          title="本月回款率"
          value={`${(overview?.monthlyCollectionRate ?? 0).toFixed(1)}%`}
          color="text-emerald-600"
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Monthly Trends */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">
            月度趋势
          </h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={data?.monthlyTrends ?? []}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar
                dataKey="newContracts"
                name="新合同"
                fill="#10b981"
                radius={[4, 4, 0, 0]}
              />
              <Bar
                dataKey="newCustomers"
                name="新客户"
                fill="#3b82f6"
                radius={[4, 4, 0, 0]}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Contract Status Distribution */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">
            合同状态分布
          </h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={statusData}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={100}
                paddingAngle={2}
                dataKey="value"
                label={({ name, value }) => `${name}: ${value}`}
              >
                {statusData.map((_, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={PIE_COLORS[index % PIE_COLORS.length]}
                  />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Alerts & Due Contracts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Overdue Alerts */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">
            ⚠️ 逾期预警
          </h3>
          {data?.overdueAlerts && data.overdueAlerts.length > 0 ? (
            <div className="space-y-3">
              {data.overdueAlerts.slice(0, 5).map((alert) => (
                <div
                  key={alert.contractId}
                  className="flex items-center justify-between p-3 bg-red-50 rounded-lg"
                >
                  <div>
                    <p className="text-sm font-medium text-slate-800">
                      {alert.contractNo}
                    </p>
                    <p className="text-xs text-slate-500">
                      {alert.customerName} · 逾期 {alert.overdueDays} 天
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-semibold text-red-600">
                      ¥{alert.overdueAmount.toLocaleString()}
                    </p>
                    <span
                      className={`text-xs px-2 py-0.5 rounded-full ${
                        alert.riskLevel === "HIGH"
                          ? "bg-red-100 text-red-700"
                          : alert.riskLevel === "MEDIUM"
                            ? "bg-amber-100 text-amber-700"
                            : "bg-yellow-100 text-yellow-700"
                      }`}
                    >
                      {alert.riskLevel}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-slate-400 text-sm">暂无逾期合同</p>
          )}
        </div>

        {/* Upcoming Due Contracts */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">
            📅 近期到期合同
          </h3>
          {data?.upcomingDueContracts &&
          data.upcomingDueContracts.length > 0 ? (
            <div className="space-y-3">
              {data.upcomingDueContracts.slice(0, 5).map((contract) => (
                <div
                  key={contract.contractId}
                  className="flex items-center justify-between p-3 bg-blue-50 rounded-lg"
                >
                  <div>
                    <p className="text-sm font-medium text-slate-800">
                      {contract.contractNo}
                    </p>
                    <p className="text-xs text-slate-500">
                      {contract.customerName} · 到期 {contract.dueDate}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-semibold text-blue-600">
                      ¥{contract.remainingPrincipal.toLocaleString()}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-slate-400 text-sm">近期无到期合同</p>
          )}
        </div>
      </div>
    </div>
  );
}
