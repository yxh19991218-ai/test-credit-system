/** 侧边导航栏组件 */
import { NavLink } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

const navItems = [
  { to: "/dashboard", label: "仪表盘", icon: "📊" },
  { to: "/customers", label: "客户管理", icon: "👥" },
  { to: "/applications", label: "贷款申请", icon: "📋" },
  { to: "/contracts", label: "合同管理", icon: "📄" },
  { to: "/products", label: "产品配置", icon: "🏷️" },
];

export default function Sidebar() {
  const { user, logout } = useAuth();

  return (
    <aside className="w-64 bg-slate-900 text-white flex flex-col h-screen">
      {/* Logo */}
      <div className="p-6 border-b border-slate-700">
        <h1 className="text-xl font-bold tracking-wide">
          <span className="text-emerald-400">Credit</span>Admin
        </h1>
        <p className="text-xs text-slate-400 mt-1">信用管理系统 v2.0</p>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4 space-y-1">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? "bg-emerald-500/20 text-emerald-400"
                  : "text-slate-300 hover:bg-slate-800 hover:text-white"
              }`
            }
          >
            <span className="text-lg">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>

      {/* User Info */}
      <div className="p-4 border-t border-slate-700">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-8 h-8 rounded-full bg-emerald-500 flex items-center justify-center text-sm font-bold">
            {user?.username.charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="text-sm font-medium">{user?.username}</p>
            <p className="text-xs text-slate-400">{user?.role}</p>
          </div>
        </div>
        <button
          onClick={logout}
          className="w-full text-left text-sm text-slate-400 hover:text-white transition-colors px-2 py-1"
        >
          ← 退出登录
        </button>
      </div>
    </aside>
  );
}
