/**
 * 类型安全的请求封装。
 *
 * 在 axios 客户端之上提供一层泛型包装，自动解包 `ApiResponse<T>` 中的 `data`，
 * 并将业务错误（code !== 0）转为 `ApiError` 抛出，消除各 API 模块中的重复类型标注。
 */

import apiClient from "./client";
import type { AxiosRequestConfig } from "axios";
import type { ApiResponse, PageData, PageParams } from "./types";
import { ApiError } from "./types";

/** 从 ApiResponse<T> 中解出 T，非零 code 抛 ApiError */
async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const { data: body } = await promise;
  if (body.code !== 0) {
    throw new ApiError(body.code, body.message, body.data);
  }
  return body.data;
}

export const request = {
  get: <T>(url: string, params?: Record<string, unknown>) =>
    unwrap<T>(apiClient.get<ApiResponse<T>>(url, { params })),

  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(apiClient.post<ApiResponse<T>>(url, data, config)),

  put: <T>(url: string, data?: unknown) =>
    unwrap<T>(apiClient.put<ApiResponse<T>>(url, data)),

  patch: <T>(url: string, data?: unknown) =>
    unwrap<T>(apiClient.patch<ApiResponse<T>>(url, data)),

  delete: <T>(url: string, params?: Record<string, unknown>) =>
    unwrap<T>(apiClient.delete<ApiResponse<T>>(url, { params })),
};

/** 分页查询快捷方法：返回 PageData<T> */
export function pageGet<T>(
  url: string,
  params?: PageParams & Record<string, unknown>,
): Promise<PageData<T>> {
  return request.get<PageData<T>>(url, params as Record<string, unknown>);
}
