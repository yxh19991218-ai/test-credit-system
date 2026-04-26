/**
 * Cloudflare Pages Function — API 反向代理
 *
 * 将 /credit-system/* 的请求转发到后端服务器。
 * Pages Functions 与站点部署在一起，无需单独部署 Worker。
 *
 * 使用方式:
 *   1. 将后端部署到 Railway / Fly.io / VPS
 *   2. 在 Cloudflare Dashboard 中设置环境变量 API_BACKEND_URL
 *   3. 重新部署 Pages 即可生效
 */

export async function onRequest(context) {
  const { request, env } = context;
  const url = new URL(request.url);
  const path = url.pathname;
  const method = request.method;

  // 只代理 /credit-system/ 路径
  if (!path.startsWith("/credit-system/")) {
    return new Response("Not Found", { status: 404 });
  }

  // 从环境变量读取后端地址，部署后在 Cloudflare Dashboard 中设置
  const API_BACKEND = env.API_BACKEND_URL || "http://localhost:8080";

  // 构造后端请求
  const backendUrl = `${API_BACKEND}${path}${url.search}`;

  // 处理 OPTIONS 预检请求
  if (method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods":
          "GET, POST, PUT, DELETE, PATCH, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
        "Access-Control-Max-Age": "86400",
      },
    });
  }

  // 转发请求到后端
  const response = await fetch(backendUrl, {
    method,
    headers: request.headers,
    body: method !== "GET" && method !== "HEAD" ? request.body : undefined,
  });

  return response;
}
