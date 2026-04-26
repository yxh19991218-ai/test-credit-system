# Credit System — 信用管理系统

全栈信用管理系统，基于 Spring Boot 3.4 + React 19 构建。

## 技术栈

| 层级     | 技术                                                                                      |
| -------- | ----------------------------------------------------------------------------------------- |
| **后端** | Spring Boot 3.4.4, Java 17, Spring Security 6 + JWT, Spring Data JPA, QueryDSL, MySQL 8.0 |
| **前端** | Vite 6, React 19, TypeScript, Tailwind CSS v4, TanStack Query, Recharts                   |
| **部署** | Docker Compose / Cloudflare Pages + Workers                                               |

## 快速开始

### 本地开发

```bash
# 后端
mvn spring-boot:run

# 前端
cd credit-admin && npm run dev
```

### Docker 部署

```bash
docker compose up -d
```

## Cloudflare 部署

本项目支持两种 Cloudflare 部署模式：

| 模式           | 前端             | 后端 API                     | 适用场景     |
| -------------- | ---------------- | ---------------------------- | ------------ |
| **完整模式**   | Cloudflare Pages | Railway/Fly.io + Worker 代理 | 生产环境     |
| **纯前端模式** | Cloudflare Pages | ❌ 无后端                    | 演示/UI 展示 |

---

### 模式一：完整部署（前端 + 后端 API）

#### 步骤 1：部署后端到云平台

Spring Boot 需要 JVM 环境，推荐部署到以下平台：

| 平台                            | 特点               | 配置参考                       |
| ------------------------------- | ------------------ | ------------------------------ |
| [Railway](https://railway.app/) | 一键部署，免费额度 | 连接 GitHub，选择 `Dockerfile` |
| [Fly.io](https://fly.io/)       | 全球边缘部署       | 使用 `Dockerfile` 部署         |
| 任意 VPS                        | 完全控制           | `docker compose up -d`         |

部署后你会得到一个后端 URL，例如 `https://credit-system-backend.railway.app`。

#### 步骤 2：部署 Cloudflare Worker（API 反向代理）

```bash
# 安装 Wrangler CLI
npm install -g wrangler

# 登录 Cloudflare
wrangler login

# 修改 wrangler.toml，将 API_BACKEND_URL 设为后端实际地址
# 取消 routes 的注释，填入你的域名和 Zone ID

# 部署 Worker
wrangler deploy
```

#### 步骤 3：部署前端到 Cloudflare Pages

1. 在 [Cloudflare Dashboard](https://dash.cloudflare.com/) → **Workers & Pages** → **创建** → **Pages**
2. 选择 **连接到 Git** → 选择 `test-credit-system` 仓库
3. 配置构建设置:
   - **项目名称**: `credit-system`
   - **生产分支**: `main`
   - **根目录**: `credit-admin`
   - **构建命令**: `npm run build`
   - **构建输出目录**: `dist`
4. 点击 **保存并部署**

#### 步骤 4：配置 API 代理（Pages Function）

在 `credit-admin/functions/` 目录下创建 API 代理，将 `/credit-system/*` 请求转发到 Worker 或直接转发到后端：

<details>
<summary>点击展开 Pages Functions 配置</summary>

```bash
# 创建 functions 目录
mkdir -p credit-admin/functions

# 创建 API 代理函数
cat > credit-admin/functions/credit-system/[[path]].ts << 'EOF'
// Cloudflare Pages Function — API 反向代理
// 将 /credit-system/* 请求转发到后端服务器

const API_BACKEND = "https://credit-system-backend.railway.app"; // 改为你的后端地址

export async function onRequest(context) {
  const { request } = context;
  const url = new URL(request.url);
  const path = url.pathname;
  const method = request.method;

  // 构造后端请求
  const backendUrl = `${API_BACKEND}${path}${url.search}`;

  // 转发请求（保留原始方法、请求头和 body）
  const response = await fetch(backendUrl, {
    method,
    headers: request.headers,
    body: method !== "GET" && method !== "HEAD" ? request.body : undefined,
  });

  return response;
}
EOF
```

</details>

> **注意**: 如果使用 Pages Functions，则不需要单独部署 Worker。Pages Functions 是 Cloudflare Pages 内置的无服务器函数，与 Pages 站点部署在一起。

---

### 模式二：纯前端部署（演示模式，无后端 API）

仅部署前端界面，适用于 UI 展示或原型演示：

1. 在 Cloudflare Dashboard 中创建 Pages 并连接 GitHub 仓库
2. 使用相同的构建设置（根目录: `credit-admin`，构建命令: `npm run build`，输出目录: `dist`）
3. 部署后前端可以正常显示页面，但 API 请求会失败

---

### 环境变量配置

如果需要在 Pages 中配置环境变量（如 API 地址），在 Cloudflare Dashboard 中设置：

| 变量名              | 说明         | 示例值           |
| ------------------- | ------------ | ---------------- |
| `VITE_API_BASE_URL` | API 基础地址 | `/credit-system` |

## 项目结构

```
credit-system/
├── src/                          # 后端源码
│   ├── main/java/com/credit/system/
│   │   ├── config/               # 配置类 (Security, CORS, JWT)
│   │   ├── controller/           # REST 控制器
│   │   ├── domain/               # 实体类
│   │   ├── dto/                  # 数据传输对象
│   │   ├── repository/           # 数据仓库
│   │   ├── service/              # 业务逻辑
│   │   └── util/                 # 工具类
│   └── resources/application.yml
├── credit-admin/                 # 前端源码
│   ├── src/
│   │   ├── api/                  # API 客户端
│   │   ├── components/           # 通用组件
│   │   ├── hooks/                # React Hooks
│   │   └── pages/                # 页面组件
│   └── package.json
├── Dockerfile                    # 后端 Docker 构建
├── docker-compose.yml            # Docker Compose 部署
├── wrangler.toml                 # Cloudflare Workers 配置
└── cloudflare-pages.toml         # Cloudflare Pages 配置
```
