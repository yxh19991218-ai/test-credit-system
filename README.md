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

### 前端 — Cloudflare Pages

1. 在 [Cloudflare Dashboard](https://dash.cloudflare.com/) → **Workers & Pages** → **创建 Pages**
2. 选择 **连接到 Git** → 选择 `test-credit-system` 仓库
3. 配置:
   - **根目录**: `/credit-admin`
   - **构建命令**: `npm run build`
   - **构建输出目录**: `dist`
4. 点击 **保存并部署**

### 后端 API — Cloudflare Workers (反向代理)

由于 Spring Boot 需要 JVM 环境，有两种方案:

**方案 A: 后端部署到云平台 + Worker 代理**

1. 将后端部署到 [Railway](https://railway.app/)、[Fly.io](https://fly.io/) 或任意 VPS
2. 修改 `wrangler.toml` 中的 `API_BACKEND_URL` 为实际地址
3. 部署 Worker:
   ```bash
   npm install -g wrangler
   cd wrangler && wrangler deploy
   ```

**方案 B: 纯前端部署 (演示模式)**

- 仅部署前端 Pages，使用 Mock 数据或本地后端
- 修改 `credit-admin/src/api/client.ts` 中的 baseURL

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
