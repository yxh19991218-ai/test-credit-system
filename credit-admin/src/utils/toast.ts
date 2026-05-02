/**
 * Toast 通知工具 —— 替代 alert/confirm/prompt 原生弹窗。
 *
 * 使用 react-hot-toast 提供美观的非阻塞通知。
 * 使用 window.confirm/prompt 的替代方案通过自定义弹窗实现。
 */
import toast from "react-hot-toast";

/** 成功通知 */
export function toastSuccess(message: string) {
  toast.success(message, {
    icon: "✅",
    duration: 3000,
  });
}

/** 错误通知 */
export function toastError(message: string) {
  toast.error(message, {
    icon: "❌",
    duration: 4000,
  });
}

/** 信息通知 */
export function toastInfo(message: string) {
  toast(message, {
    icon: "ℹ️",
    duration: 3000,
  });
}

/** 警告通知 */
export function toastWarning(message: string) {
  toast(message, {
    icon: "⚠️",
    duration: 3500,
    style: { background: "#fef3c7", color: "#92400e" },
  });
}

/**
 * 确认对话框 —— 返回 Promise<boolean>
 * 使用 window.confirm 实现（后续可替换为自定义 Modal 组件）
 */
export function confirmAction(message: string): boolean {
  return window.confirm(message);
}

/**
 * 输入对话框 —— 返回 Promise<string | null>
 * 使用 window.prompt 实现（后续可替换为自定义 Modal 组件）
 */
export function promptInput(message: string, defaultValue?: string): string | null {
  return window.prompt(message, defaultValue);
}
