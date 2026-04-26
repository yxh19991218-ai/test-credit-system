/** 仪表盘数据类型定义 */

export interface DashboardResponse {
  overview: Overview;
  monthlyTrends: MonthlyTrend[];
  productDistribution: ProductDistribution[];
  contractStatusDistribution: Record<string, number>;
  upcomingDueContracts: ContractBrief[];
  overdueAlerts: OverdueAlert[];
}

export interface Overview {
  totalCustomers: number;
  activeContracts: number;
  pendingApplications: number;
  overdueContracts: number;
  totalLoanAmount: number;
  totalRemainingPrincipal: number;
  totalOverdueAmount: number;
  monthlyCollectionRate: number;
}

export interface MonthlyTrend {
  month: string;
  newContracts: number;
  loanAmount: number;
  repaymentAmount: number;
  newCustomers: number;
}

export interface ProductDistribution {
  productName: string;
  contractCount: number;
  totalAmount: number;
}

export interface ContractBrief {
  contractId: number;
  contractNo: string;
  customerName: string;
  totalAmount: number;
  remainingPrincipal: number;
  dueDate: string;
  status: string;
}

export interface OverdueAlert {
  contractId: number;
  contractNo: string;
  customerName: string;
  overdueDays: number;
  overdueAmount: number;
  riskLevel: string;
}
