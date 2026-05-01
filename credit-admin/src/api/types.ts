/**
 * API 通用类型定义。
 *
 * 提供统一的响应包装、分页、错误类型，消除各 API 模块中的重复类型定义。
 */

/** 后端统一响应格式 */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

/** 分页响应 */
export interface PageData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** 分页请求参数 */
export interface PageParams {
  page?: number;
  size?: number;
}

/** API 业务错误（code !== 0 时抛出） */
export class ApiError extends Error {
  readonly code: number;
  readonly details: unknown;

  constructor(code: number, message: string, details?: unknown) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.details = details;
  }
}
