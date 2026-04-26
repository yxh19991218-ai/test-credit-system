/**
 * Cloudflare Worker — 后端 API 反向代理
 *
 * 功能:
 * 1. 将 /credit-system/* 请求转发到后端服务器
 * 2. 自动处理 CORS 头
 * 3. 可选缓存策略
 */

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;

    // 从环境变量读取后端地址
    const API_BACKEND = env.API_BACKEND_URL || "http://localhost:8080";

    // 只代理 /credit-system/ 路径
    if (!path.startsWith("/credit-system/")) {
      return new Response("Not Found", { status: 404 });
    }

    // 构造后端请求
    const backendUrl = `${API_BACKEND}${path}${url.search}`;
    const backendRequest = new Request(backendUrl, {
      method,
      headers: request.headers,
      body: method !== "GET" && method !== "HEAD" ? request.body : undefined,
    });

    // 转发请求
    const response = await fetch(backendRequest);

    // 添加 CORS 头
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, PATCH, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
      "Access-Control-Max-Age": "86400",
    };

    // 处理 OPTIONS 预检请求
    if (method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: corsHeaders,
      });
    }

    // 添加 CORS 头到响应
    const newResponse = new Response(response.body, response);
    Object.entries(corsHeaders).forEach(([key, value]) => {
      newResponse.headers.set(key, value);
    });

    return newResponse;
  },
};
