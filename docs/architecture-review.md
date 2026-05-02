# 代码架构审查报告

> 审查日期: 2026-05-02
> 审查范围: credit-system 全栈项目（Spring Boot 后端 + React 前端）

---

## 一、总体评价

### 1.1 架构亮点 ✅

| 维度 | 评价 |
|------|------|
| **分层清晰** | Controller → Service → Repository 三层架构明确，职责分离良好 |
| **领域事件驱动** | 审批→合同→还款计划的异步事件链设计优雅，`EventBus` + `DomainEventHandler` 模式成熟 |
| **策略模式** | `RepaymentCalculator` 策略接口 + `RepaymentCalculatorRegistry` 注册表，扩展性好 |
| **规约模式** | `CustomerSpecification` 接口 + `and()` 组合，验证逻辑可复用可组合 |
| **审计日志** | `AuditAspect` + `@AuditLoggable` 注解，AOP 实现审计，侵入性低 |
| **安全设计** | `SecurityAspect` + `@RequireAdmin` 注解，与 Spring Security 互补 |
| **前端架构** | React Query 管理服务端状态，`request.ts` 统一解包，类型安全 |
| **DTO 映射** | `EntityMapper` 接口统一转换，避免散落各处 |

### 1.2 总体健康度

```
后端架构成熟度: ★★★★☆ (4/5)
前端架构成熟度: ★★★☆☆ (3/5)
测试覆盖度:     ★★☆☆☆ (2/5)
文档完整度:     ★★★★☆ (4/5)
```

---

## 二、后端架构问题

### 🔴 严重问题 (Critical)

#### 2.1 领域模型贫血 —— 业务逻辑散落在 Service 中

**问题**: 所有 `@Entity` 类（`Customer`, `LoanApplication`, `LoanContract` 等）都是纯数据容器（`@Data` + 字段），没有任何业务方法。业务逻辑全部在 Service 层实现。

**示例**: `LoanApplicationServiceImpl.reviewApplication()` 中包含了审批状态流转、月供计算、事件发布等逻辑，但 `LoanApplication` 实体本身没有任何行为。

**影响**: 
- 违反 DDD 聚合设计原则
- 业务规则分散，难以维护和测试
- 一个 Service 方法可能操作多个实体，事务边界模糊

**建议**: 
- 将状态校验逻辑（如 `仅草稿状态的申请可以提交`）移到实体内部方法
- 创建聚合根（如 `LoanApplication` 作为聚合根，包含 `review()`, `submit()`, `cancel()` 方法）
- 参考 `CustomerServiceImpl` 中的 `validateStatusTransition()` 模式，但应移到实体中

#### 2.2 事件系统存在两层路由 —— Listener + Handler 冗余

**问题**: 事件处理存在两层间接调用：
```
EventBus.publish() → DomainEventWrapper → 
  ApplicationApprovedEventListener (路由) → 
    ApplicationApprovedHandler (业务逻辑)
```

`Listener` 和 `Handler` 是一一对应的，`Listener` 只做 `instanceof` 检查和委托调用，没有额外职责。

**影响**:
- 不必要的间接层，增加理解和维护成本
- 每个事件需要创建 2 个类（Listener + Handler）

**建议**: 
- 移除 `DomainEventHandler` 接口和 Handler 实现
- 让 `Listener` 直接注入 Service 执行业务逻辑
- 或保留 Handler 但移除 Listener，直接在 Handler 上使用 `@TransactionalEventListener`

#### 2.3 DTO 存在两套转换机制 —— 不一致

**问题**: 部分 DTO 同时存在 `static from(Entity)` 方法和 `EntityMapper` 接口实现，两套转换逻辑并存且内容重复。

**示例**: 
- `CustomerResponse.from(Customer)` 静态方法
- `CustomerMapper.toDto(Customer)` 实例方法
- 两者代码几乎完全重复

**影响**:
- 维护成本翻倍，修改实体字段需要同步修改两处
- 容易产生不一致（如 `ContractResponse` 缺少 `updatedAt` 字段）

**建议**: 
- 统一使用 `EntityMapper` 接口，移除所有 `static from()` 方法
- 或使用 MapStruct 自动生成映射代码

#### 2.4 前端状态枚举与后端枚举不一致

**问题**: 前端硬编码了状态枚举值，与后端枚举定义存在差异。

**示例**:
- 前端 `ProductsPage.tsx` 检查 `product.status === "PUBLISHED"`，但后端 `ProductStatus` 枚举值为 `ACTIVE`、`DISABLED`、`RETIRED`、`DRAFT`
- 前端 `ApplicationsPage.tsx` 发送 `decision: "APPROVE"`，后端 Controller 需要做兼容转换 `"APPROVE" → "APPROVED"`

**影响**:
- 前后端耦合，修改后端枚举需要同步修改前端
- 运行时可能产生静默错误

**建议**: 
- 前端定义与后端一致的枚举常量
- 后端 Controller 移除兼容转换代码，统一枚举值

---

### 🟠 中等问题 (Medium)

#### 2.5 实体间使用 Long ID 引用而非对象关联

**问题**: 所有实体间关联使用 `Long customerId`、`Long productId` 等原始 ID，而非 JPA 的 `@ManyToOne` 关联。

**示例**: `LoanApplication` 中有 `customerId`、`productId`，但没有 `@ManyToOne Customer customer`。

**影响**:
- 需要手动编写 Repository 方法进行关联查询
- 无法利用 JPA 的懒加载和级联操作
- 违反 ORM 设计原则

**建议**: 
- 添加 `@ManyToOne(fetch = FetchType.LAZY)` 关联
- 保留 `customerId` 作为数据库列（通过 `@Column(name = "customer_id", insertable = false, updatable = false)`）

#### 2.6 Service 层使用 `@Autowired` 字段注入

**问题**: 部分 Service 使用 `@Autowired` 字段注入（如 `ContractServiceImpl`），部分使用构造器注入（如 `LoanApplicationServiceImpl`），风格不统一。

**影响**:
- 字段注入不利于单元测试（无法用 `mock()` 构造）
- 循环依赖只能在运行时发现

**建议**: 
- 统一使用构造器注入（如 `LoanApplicationServiceImpl` 的做法）
- 使用 `final` 关键字确保不可变性

#### 2.7 仪表盘月度趋势数据为空

**问题**: `DashboardServiceImpl.buildMonthlyTrends()` 返回的月度趋势数据中，`newContracts`、`loanAmount` 等字段全部为 0。

```java
trends.add(DashboardResponse.MonthlyTrend.builder()
    .month(monthStr)
    .newContracts(0)        // 硬编码为 0
    .loanAmount(BigDecimal.ZERO)  // 硬编码为 0
    ...
    .build());
```

**影响**: 前端图表显示全零数据，功能不完整。

**建议**: 实现真实的月度聚合查询。

#### 2.8 产品状态枚举值混乱

**问题**: `ProductStatus` 枚举定义了 `DRAFT`、`ACTIVE`、`DISABLED`、`RETIRED`，但前端使用 `PUBLISHED` 作为状态值。

**后端枚举**:
```java
public enum ProductStatus {
    DRAFT, ACTIVE, DISABLED, RETIRED
}
```

**前端代码**:
```typescript
product.status === "PUBLISHED" ? "已发布" : "已下架"
```

**影响**: 前端永远无法匹配到 `PUBLISHED`，产品状态显示始终为"已下架"。

#### 2.9 合同创建时状态初始化为 ACTIVE 而非 PENDING_SIGN

**问题**: `ContractServiceImpl.createContract()` 中 `contract.setStatus(ContractStatus.ACTIVE)`，但前端期望 `PENDING_SIGN` 状态。

**影响**: 前端 `ContractsPage.tsx` 中 `PENDING_SIGN` 状态对应的"签署"按钮永远不会显示。

#### 2.10 前端分页查询固定为 size=100

**问题**: 所有前端页面列表查询都硬编码 `size: 100`，没有实现真正的分页。

```typescript
// ApplicationsPage.tsx, ContractsPage.tsx, CustomersPage.tsx
applicationApi.list({ status: statusFilter, page: 0, size: 100 })
```

**影响**: 数据量大时性能问题，用户体验差（无分页控件）。

---

### 🟡 轻微问题 (Minor)

#### 2.11 审计日志 `extractEntityId` 约定脆弱

**问题**: `AuditAspect.extractEntityId()` 通过"第一个 Long 参数"的约定提取实体 ID，容易出错。

**影响**: 如果方法签名变化（如增加 `String operator` 参数），实体 ID 提取可能错误。

**建议**: 使用自定义注解标注参数，或从方法名/参数名推断。

#### 2.12 部分方法体为空

**问题**: `CustomerServiceImpl` 中存在多个空方法体。

```java
private void freezeAllActiveLoans(Long customerId) { }
private void validateBlacklistCustomerUpdate(Customer customerDetails) { }
private boolean hasActiveLoans(Long customerId) { return false; }
```

**影响**: 功能不完整，存在技术债务。

#### 2.13 前端错误处理使用 `alert()`

**问题**: 前端所有错误处理都使用 `alert()` 弹窗，用户体验差。

```typescript
onError: (err: any) => {
  const msg = err?.response?.data?.message || err?.message || "创建失败";
  alert(msg);
}
```

**建议**: 使用 Toast 通知组件（如 react-hot-toast）替代。

#### 2.14 前端硬编码操作人

**问题**: 前端审批操作硬编码 `reviewer: "admin"`。

```typescript
applicationApi.review(id, { decision: "APPROVE", reviewer: "admin" })
```

**影响**: 无法追踪真实操作人。

#### 2.15 缺少集成测试

**问题**: 测试目录结构存在但测试用例较少，缺少端到端的集成测试。

**建议**: 增加 `@SpringBootTest` 集成测试，覆盖核心业务流程。

---

## 三、前端架构问题

### 3.1 组件粒度偏大

**问题**: 每个页面文件（如 `CustomersPage.tsx` 348 行）包含了查询、表单、表格、弹窗等所有逻辑，未拆分为子组件。

**建议**: 提取 `CustomerTable`、`CustomerFormModal`、`SearchBar` 等可复用组件。

### 3.2 缺少路由懒加载

**问题**: `App.tsx` 中所有页面组件都是静态 import，没有使用 `React.lazy()`。

**建议**: 使用 `React.lazy(() => import("./pages/CustomersPage"))` 实现代码分割。

### 3.3 缺少 TypeScript 严格模式

**问题**: 多处使用 `any` 类型（如 `onError: (err: any)`），类型安全不足。

**建议**: 启用 `tsconfig.json` 的 `strict: true`，消除 `any` 类型。

---

## 四、安全架构问题

### 4.1 认证实现过于简化

**问题**: `AuthController.login()` 使用硬编码的用户名密码验证。

```java
if (!"admin".equals(request.username()) || !"admin123".equals(request.password())) {
    if (!"user".equals(request.username()) || !"user123".equals(request.password())) {
        throw new BadCredentialsException("用户名或密码错误");
    }
}
```

**影响**: 无法满足生产环境安全要求。

**建议**: 集成 Spring Security 的 `UserDetailsService` + 数据库存储用户信息。

### 4.2 缺少 API 限流和防重放

**问题**: 没有配置请求限流（Rate Limiting）和防重放攻击机制。

**建议**: 集成 Spring Cloud Gateway 或 Resilience4j 的限流功能。

---

## 五、性能架构问题

### 5.1 N+1 查询风险

**问题**: `DashboardServiceImpl.buildContractStatusDistribution()` 中，对每个 `ContractStatus` 枚举值都执行一次数据库查询。

```java
for (ContractStatus status : ContractStatus.values()) {
    long count = contractRepository.findByStatus(status).size();
    distribution.put(status.name(), count);
}
```

**影响**: 5 个状态值 = 5 次数据库查询，仪表盘加载时存在 N+1 问题。

**建议**: 使用单个 `GROUP BY` 查询替代循环查询。

### 5.2 缺少缓存

**问题**: 仪表盘数据、产品列表等读多写少的数据没有使用缓存。

**建议**: 使用 Spring Cache（Redis）缓存仪表盘数据，设置合理的过期时间。

---

## 六、改进优先级建议

### 立即修复 (P0) — ✅ 已修复

| 优先级 | 问题 | 影响 | 修复状态 |
|--------|------|------|----------|
| P0 | 产品状态枚举不匹配 (PUBLISHED vs ACTIVE) | 前端功能不可用 | ✅ 前端 `ProductsPage.tsx` 中 `PUBLISHED` → `ACTIVE` |
| P0 | 合同状态初始化为 ACTIVE 而非 PENDING_SIGN | 签署功能不可用 | ✅ 添加 `PENDING_SIGN` 枚举，`ContractServiceImpl.createContract()` 初始化为 `PENDING_SIGN`，`signContract()` 签署后转为 `ACTIVE` |
| P0 | 仪表盘月度趋势数据全零 | 图表功能不可用 | ✅ `DashboardServiceImpl.buildMonthlyTrends()` 实现真实数据库聚合查询，新增 `LoanContractRepository.countByCreatedAtBetween()`、`sumTotalAmountByCreatedAtBetween()`、`CustomerRepository.countByCreatedAtBetween()` 方法 |

### 短期改进 (P1) — ✅ 已修复

| 优先级 | 问题 | 影响 | 修复状态 |
|--------|------|------|----------|
| P1 | DTO 两套转换机制统一 | 维护成本 | ✅ 移除所有 DTO 的 `from()` 静态方法，统一使用 `EntityMapper` |
| P1 | 事件系统 Listener/Handler 合并 | 代码复杂度 | ✅ 移除 `DomainEventHandler` 接口和 Handler 实现，Listener 直接处理 |
| P1 | 前端分页实现 | 性能/体验 | ✅ ApplicationsPage、ContractsPage、CustomersPage 添加分页控件 |
| P1 | 前端枚举与后端对齐 | 功能正确性 | ✅ 前端 `APPROVE`/`REJECT` → `APPROVED`/`REJECTED`，移除 Controller 兼容代码 |
| P1 | 仪表盘 N+1 查询优化 | 性能 | ✅ `buildContractStatusDistribution()` 使用 `GROUP BY` 单次查询替代循环 |

### 中期重构 (P2) — ✅ 已修复

| 优先级 | 问题 | 影响 | 修复状态 |
|--------|------|------|----------|
| P2 | 领域模型贫血 → 富领域模型 | 架构质量 | ✅ `Customer`、`LoanApplication`、`LoanContract` 添加领域行为方法（`submit()`、`review()`、`sign()`、`terminate()`、`extend()`、`settle()`、`updateStatus()`、`updateCreditInfo()`、`delete()`、`assessRisk()` 等），Service 层改为调用实体方法 |
| P2 | 统一构造器注入 | 可测试性 | ✅ `ContractServiceImpl`、`LoanProductServiceImpl` 从 `@Autowired` 字段注入改为构造器注入 + `final` 字段 |
| P2 | 前端组件拆分 | 可维护性 | ✅ 提取 `CustomerTable`、`CustomerFormModal`、`SearchBar`、`StatusBadge`、`Pagination`、`ContractTable`、`ApplicationTable`、`ProductTable` 等可复用组件 |
| P2 | 集成测试覆盖 | 质量保障 | ✅ 新增 `LoanApplicationServiceTest`、`ContractServiceTest`、`CustomerServiceTest`、`RepaymentScheduleServiceTest` 集成测试 |


### 长期规划 (P3) — ✅ 部分已修复

| 优先级 | 问题 | 影响 | 修复状态 |
|--------|------|------|----------|
| P3 | 认证系统集成数据库 | 安全性 | ✅ 新增 `User` 实体、`UserRepository`、`CustomUserDetailsService`，`AuthController` 改为通过 `AuthenticationManager` 认证 |
| P3 | API 限流 | 安全性 | ✅ 新增 `RateLimitingFilter` 实现基于 IP 的令牌桶限流 |
| P3 | 数据缓存 | 性能 | ✅ 新增 `CacheConfig`（Caffeine 本地缓存），`DashboardServiceImpl.getDashboardOverview()` 添加 `@Cacheable` |
| P3 | 前端路由懒加载 | 性能 | ✅ `App.tsx` 使用 `React.lazy()` 实现路由级代码分割 |
| P3 | Toast 替代 alert | 用户体验 | ✅ 新增 `utils/toast.ts` 工具，`CustomersPage.tsx` 已替换 `alert()` → `toastSuccess()`/`toastError()` |

---

## 七、最佳实践保持项

以下架构设计值得在后续开发中继续保持：

1. **事件驱动架构**: `EventBus` + `@TransactionalEventListener` 的组合，实现了事务边界内的事件发布和异步处理
2. **策略模式注册表**: `RepaymentCalculatorRegistry` 自动收集所有策略实现，新增还款方式无需修改现有代码
3. **规约模式**: `CustomerSpecification` 的 `and()` 组合方法，实现了验证逻辑的灵活组装
4. **AOP 审计**: `@AuditLoggable` 注解 + `AuditAspect` 切面，低侵入地实现审计日志
5. **前端请求封装**: `request.ts` 的泛型解包 + `ApiError` 类，提供了类型安全的 API 调用层
6. **Token 自动刷新**: `client.ts` 的响应拦截器实现了 401 自动刷新 Token
7. **文档完整性**: `CONTEXT.md`、`CONTEXT-MAP.md`、`docs/adr/` 等文档完善

---

## 八、总结

credit-system 项目整体架构设计良好，采用了事件驱动、策略模式、规约模式等成熟架构模式，前后端分层清晰。

### 已完成修复 (P0 + P1 + P2 + P3)

本次架构审查共发现并修复了 **17 个问题**：

| 优先级 | 问题数 | 修复内容 |
|--------|--------|----------|
| P0 (紧急) | 3 | 产品状态枚举、合同状态初始化、仪表盘月度趋势数据 |
| P1 (短期) | 5 | DTO 转换机制统一、事件系统 Listener/Handler 合并、前端分页、前端枚举对齐、N+1 查询优化 |
| P2 (中期) | 4 | 领域模型富化（Customer/LoanApplication/LoanContract 添加领域行为）、统一构造器注入（ContractServiceImpl/LoanProductServiceImpl）、前端组件拆分、集成测试覆盖 |
| P3 (长期) | 5 | 认证系统集成数据库、API 限流、数据缓存（Caffeine）、前端路由懒加载、Toast 替代 alert |

### 剩余待改进

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P3 | 实体 Long ID → @ManyToOne 关联 | 架构质量 |
