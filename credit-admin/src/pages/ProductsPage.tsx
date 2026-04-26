/** 产品配置页面 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { productApi, type Product } from "../api/products";

/** 将后端字段名映射到前端期望的字段 */
function mapProduct(p: any): Product {
  return productApi.transformResponse(p);
}

export default function ProductsPage() {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [form, setForm] = useState({
    code: "",
    name: "",
    description: "",
    minAmount: 0,
    maxAmount: 100000,
    minTerm: 1,
    maxTerm: 36,
    interestRate: 5.0,
  });

  const { data, isLoading } = useQuery({
    queryKey: ["products"],
    queryFn: async () => {
      const res = await productApi.list();
      // 后端返回 Page<LoanProductResponse>，data.data 是 Page 对象，content 是数组
      const pageData = res.data?.data;
      if (Array.isArray(pageData)) {
        return productApi.transformListResponse(pageData);
      }
      if (pageData?.content) {
        return productApi.transformListResponse(pageData.content);
      }
      return [];
    },
  });

  /** 将前端表单转为后端 API 请求格式 */
  const toApiDto = (f: typeof form) => ({
    productCode: f.code,
    productName: f.name,
    productDescription: f.description || undefined,
    minAmount: f.minAmount,
    maxAmount: f.maxAmount,
    minTerm: f.minTerm,
    maxTerm: f.maxTerm,
    interestRate: f.interestRate / 100, // 前端显示 5.0 表示 5%，后端存 0.05
  });

  const createMutation = useMutation({
    mutationFn: (dto: typeof form) => productApi.create(toApiDto(dto)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
      setShowModal(false);
      resetForm();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, dto }: { id: number; dto: typeof form }) =>
      productApi.update(id, toApiDto(dto)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
      setShowModal(false);
      resetForm();
    },
  });

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      productApi.toggleActive(id, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["products"] }),
  });

  const resetForm = () => {
    setForm({
      code: "",
      name: "",
      description: "",
      minAmount: 0,
      maxAmount: 100000,
      minTerm: 1,
      maxTerm: 36,
      interestRate: 5.0,
    });
    setEditingProduct(null);
  };

  const openEdit = (product: Product) => {
    setEditingProduct(product);
    setForm({
      code: product.productCode,
      name: product.productName,
      description: product.productDescription ?? "",
      minAmount: product.minAmount,
      maxAmount: product.maxAmount,
      minTerm: product.minTerm,
      maxTerm: product.maxTerm,
      interestRate: product.interestRate * 100,
    });
    setShowModal(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingProduct) {
      updateMutation.mutate({ id: editingProduct.id, dto: { ...form } });
    } else {
      createMutation.mutate({ ...form });
    }
  };

  const products = data ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">产品配置</h1>
          <p className="text-slate-500 mt-1">管理贷款产品与利率</p>
        </div>
        <button
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        >
          + 新增产品
        </button>
      </div>

      {/* Product Cards */}
      {isLoading ? (
        <div className="text-center py-12 text-slate-400">加载中...</div>
      ) : products.length === 0 ? (
        <div className="text-center py-12 text-slate-400">暂无产品</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {products.map((product: Product) => (
            <div
              key={product.id}
              className={`bg-white rounded-xl shadow-sm border-2 p-6 transition-all hover:shadow-md ${
                product.status === "ACTIVE"
                  ? "border-emerald-200"
                  : "border-slate-200 opacity-70"
              }`}
            >
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h3 className="text-lg font-semibold text-slate-800">
                    {product.productName}
                  </h3>
                  {product.productDescription && (
                    <p className="text-sm text-slate-500 mt-1">
                      {product.productDescription}
                    </p>
                  )}
                </div>
                <span
                  className={`text-xs px-2 py-1 rounded-full ${
                    product.status === "PUBLISHED"
                      ? "bg-emerald-100 text-emerald-700"
                      : "bg-slate-100 text-slate-600"
                  }`}
                >
                  {product.status === "PUBLISHED" ? "已发布" : "已下架"}
                </span>
              </div>

              <div className="space-y-2 mb-4">
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">年利率</span>
                  <span className="font-semibold text-emerald-600">
                    {(product.interestRate * 100).toFixed(2)}%
                  </span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">金额范围</span>
                  <span className="text-slate-700">
                    ¥{product.minAmount.toLocaleString()} ~ ¥
                    {product.maxAmount.toLocaleString()}
                  </span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">期限范围</span>
                  <span className="text-slate-700">
                    {product.minTerm} ~ {product.maxTerm} 个月
                  </span>
                </div>
              </div>

              <div className="flex gap-2 pt-3 border-t border-slate-100">
                <button
                  onClick={() => openEdit(product)}
                  className="flex-1 text-center text-sm text-blue-600 hover:text-blue-800 py-1"
                >
                  编辑
                </button>
                <button
                  onClick={() =>
                    toggleActiveMutation.mutate({
                      id: product.id,
                      active: product.status !== "PUBLISHED",
                    })
                  }
                  className={`flex-1 text-center text-sm py-1 ${
                    product.status === "PUBLISHED"
                      ? "text-amber-600 hover:text-amber-800"
                      : "text-emerald-600 hover:text-emerald-800"
                  }`}
                >
                  {product.status === "PUBLISHED" ? "下架" : "发布"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-lg mx-4">
            <h3 className="text-lg font-semibold text-slate-800 mb-6">
              {editingProduct ? "编辑产品" : "新增产品"}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  产品代码
                </label>
                <input
                  type="text"
                  value={form.code}
                  onChange={(e) => setForm({ ...form, code: e.target.value })}
                  className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  placeholder="例如: LOAN_001"
                  required
                  disabled={!!editingProduct}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  产品名称
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
                  描述
                </label>
                <textarea
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                  className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  rows={2}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    最低金额
                  </label>
                  <input
                    type="number"
                    value={form.minAmount}
                    onChange={(e) =>
                      setForm({ ...form, minAmount: Number(e.target.value) })
                    }
                    className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    最高金额
                  </label>
                  <input
                    type="number"
                    value={form.maxAmount}
                    onChange={(e) =>
                      setForm({ ...form, maxAmount: Number(e.target.value) })
                    }
                    className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    最低期限(月)
                  </label>
                  <input
                    type="number"
                    value={form.minTerm}
                    onChange={(e) =>
                      setForm({ ...form, minTerm: Number(e.target.value) })
                    }
                    className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    最高期限(月)
                  </label>
                  <input
                    type="number"
                    value={form.maxTerm}
                    onChange={(e) =>
                      setForm({ ...form, maxTerm: Number(e.target.value) })
                    }
                    className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  年利率 (%)
                </label>
                <input
                  type="number"
                  step="0.01"
                  value={form.interestRate}
                  onChange={(e) =>
                    setForm({ ...form, interestRate: Number(e.target.value) })
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
                  {editingProduct ? "保存" : "创建"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
