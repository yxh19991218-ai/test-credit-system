/** 客户管理页面 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
    customerApi,
    type Customer,
    type CustomerRequest,
} from "../api/customers";

export default function CustomersPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);
  const [form, setForm] = useState<CustomerRequest>({
    name: "",
    idCard: "",
    phone: "",
    email: "",
    address: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["customers", search],
    queryFn: async () => {
      const res = await customerApi.list({
        keyword: search,
        page: 0,
        size: 100,
      });
      return res.data;
    },
  });

  const createMutation = useMutation({
    mutationFn: (dto: CustomerRequest) => customerApi.create(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["customers"] });
      setShowModal(false);
      resetForm();
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || "创建失败";
      alert(msg);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, dto }: { id: number; dto: CustomerRequest }) =>
      customerApi.update(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["customers"] });
      setShowModal(false);
      resetForm();
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || "更新失败";
      alert(msg);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      customerApi.delete(id, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["customers"] }),
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || "删除失败";
      alert(msg);
    },
  });

  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      customerApi.updateStatus(id, active ? "NORMAL" : "FROZEN", ""),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["customers"] }),
  });

  const resetForm = () => {
    setForm({ name: "", idCard: "", phone: "", email: "", address: "" });
    setEditingCustomer(null);
  };

  const openEdit = (customer: Customer) => {
    setEditingCustomer(customer);
    setForm({
      name: customer.name,
      idCard: customer.idCard,
      phone: customer.phone,
      email: customer.email ?? "",
      address: customer.address ?? "",
    });
    setShowModal(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingCustomer) {
      updateMutation.mutate({ id: editingCustomer.id, dto: form });
    } else {
      createMutation.mutate(form);
    }
  };

  const pageData = data?.data;
  const customers = pageData?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">客户管理</h1>
          <p className="text-slate-500 mt-1">管理所有客户信息</p>
        </div>
        <button
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        >
          + 新增客户
        </button>
      </div>

      {/* Search */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="搜索客户名称、身份证号、手机号..."
          className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none"
        />
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
                姓名
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                身份证号
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                手机号
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                邮箱
              </th>
              <th className="text-left px-6 py-3 text-sm font-semibold text-slate-600">
                状态
              </th>
              <th className="text-right px-6 py-3 text-sm font-semibold text-slate-600">
                操作
              </th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={7} className="text-center py-12 text-slate-400">
                  加载中...
                </td>
              </tr>
            ) : customers.length === 0 ? (
              <tr>
                <td colSpan={7} className="text-center py-12 text-slate-400">
                  暂无客户数据
                </td>
              </tr>
            ) : (
              customers.map((customer: Customer) => (
                <tr
                  key={customer.id}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {customer.id}
                  </td>
                  <td className="px-6 py-4 text-sm font-medium text-slate-800">
                    {customer.name}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {customer.idCard}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {customer.phone}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {customer.email ?? "-"}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`text-xs px-2 py-1 rounded-full ${
                        customer.status === "NORMAL"
                          ? "bg-emerald-100 text-emerald-700"
                          : customer.status === "DELETED"
                            ? "bg-gray-100 text-gray-500"
                            : "bg-red-100 text-red-700"
                      }`}
                    >
                      {customer.status === "NORMAL"
                        ? "正常"
                        : customer.status === "DELETED"
                          ? "已删除"
                          : "禁用"}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right space-x-2">
                    <button
                      onClick={() => openEdit(customer)}
                      className={`text-sm ${customer.status === "DELETED" ? "text-gray-400 cursor-not-allowed" : "text-blue-600 hover:text-blue-800"}`}
                      disabled={customer.status === "DELETED"}
                    >
                      编辑
                    </button>
                    {customer.status !== "DELETED" && (
                      <button
                        onClick={() =>
                          toggleStatusMutation.mutate({
                            id: customer.id,
                            active: customer.status !== "NORMAL",
                          })
                        }
                        className={`text-sm ${customer.status === "NORMAL" ? "text-amber-600 hover:text-amber-800" : "text-emerald-600 hover:text-emerald-800"}`}
                      >
                        {customer.status === "NORMAL" ? "禁用" : "启用"}
                      </button>
                    )}
                    {customer.status !== "DELETED" && (
                      <button
                        onClick={() => {
                          if (confirm("确定删除该客户？")) {
                            const reason = prompt("请输入删除原因：");
                            if (reason !== null) {
                              deleteMutation.mutate({
                                id: customer.id,
                                reason: reason || "手动删除",
                              });
                            }
                          }
                        }}
                        className="text-red-600 hover:text-red-800 text-sm"
                      >
                        删除
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-lg mx-4">
            <h3 className="text-lg font-semibold text-slate-800 mb-6">
              {editingCustomer ? "编辑客户" : "新增客户"}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  姓名
                </label>
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  身份证号
                </label>
                <input
                  type="text"
                  value={form.idCard}
                  onChange={(e) => setForm({ ...form, idCard: e.target.value })}
                  className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  手机号
                </label>
                <input
                  type="text"
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                  className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  邮箱
                </label>
                <input
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  地址
                </label>
                <input
                  type="text"
                  value={form.address}
                  onChange={(e) =>
                    setForm({ ...form, address: e.target.value })
                  }
                  className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>
              <div className="flex justify-end gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:text-slate-800"
                >
                  取消
                </button>
                <button
                  type="submit"
                  className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-2 rounded-lg text-sm font-medium"
                >
                  {editingCustomer ? "保存" : "创建"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
