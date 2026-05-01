# 信用系统领域词汇表

## 核心概念

### 客户（Customer）
- **定义**: 申请贷款的个人
- **关键属性**: 身份证、手机、邮箱、职业、月收入、征信分数
- **状态**: NORMAL、BLACKLISTED、FROZEN

### 贷款产品（LoanProduct）
- **定义**: 金融产品的配置，定义贷款条件和规则
- **关键属性**: 产品代码、名称、金额范围、期限范围、最低征信分数、最低月收入
- **状态**: ACTIVE、INACTIVE、ARCHIVED
- **角色**: 审批聚合使用，作为申请的参考依据

### 贷款申请（LoanApplication）
**聚合根**：审批聚合的顶级实体
- **定义**: 客户对产品的申请请求
- **生命周期**: DRAFT → PENDING → APPROVED/REJECTED/CANCELLED
- **APPROVED → COMPLETED**: 申请审批通过后由事件驱动自动生成合同
- **关键属性**: 
  - 申请基本信息：申请金额、期限、用途
  - 审批结果：批准金额、批准期限、利率、月还款额
  - 审核信息：审核人、审核时间、审核意见
- **职责**:
  - 验证产品规则（金额、期限、征信、收入）
  - 记录审批决策和结果
  - 发布 `ApplicationApprovedEvent` 触发合同创建

### 贷款合同（LoanContract）
**聚合根**：履约聚合的顶级实体
- **定义**: 审批通过后的正式借款协议
- **来源**: 由 ApplicationApprovedEvent 触发自动生成
- **生命周期**: ACTIVE → SIGNED → SETTLED/BAD_DEBT/TERMINATED
- **关键属性**:
  - 合同号（唯一标识）
  - applicationId（指向审批聚合，单向引用）
  - 放款信息：金额、期限、利率、还款方式
  - 履约信息：开始日期、结束日期、剩余本金、已还期数
  - 生命周期管理：展期月数、终止原因
- **职责**:
  - 签署和管理借款协议
  - 追踪放款和还款进度
  - 发布 `ContractCreatedEvent` 触发还款计划生成

### 还款计划（RepaymentSchedule）
**聚合根**：还款聚合的顶级实体
- **定义**: 根据合同条款生成的分期还款安排
- **来源**: 由 ContractCreatedEvent 触发自动生成
- **关键属性**:
  - 还款方式：等额本息、等额本金、先息后本、气球贷、到期一次性
  - 还款周期和金额
  - 已付期数、逾期情况
- **职责**:
  - 按还款方式生成分期明细
  - 处理还款和逾期标记
  - 修改计划期限（展期）

## 聚合边界

| 聚合 | 根实体 | 主要职责 |
|------|--------|---------|
| **审批聚合** | LoanApplication | 申请创建、提交、审核、取消 |
| **履约聚合** | LoanContract | 放款、签署、展期、结清 |
| **还款聚合** | RepaymentSchedule | 还款计划、还款处理、逾期处理 |

## 事件驱动流程

```
审批通过事件链：
LoanApplicationServiceImpl.reviewApplication(APPROVED)
  ↓
发布 ApplicationApprovedEvent
  ↓
ApplicationApprovedEventListener.onApplicationApproved()
  ↓
ContractService.createContractFromApplication()
  ↓
发布 ContractCreatedEvent
  ↓
ContractCreatedEventListener.onContractCreated()
  ↓
RepaymentScheduleService.generateSchedule()
```

## 关键设计原则

1. **单向关联**: LoanContract 仅通过 applicationId 引用 LoanApplication，不反向持有
2. **事务一致性**: 事件发布在事务提交后，保证审批结果稳定后再启动履约流程
3. **聚合独立**: 各聚合在自身事务边界内操作，通过事件进行跨聚合通信
4. **幂等性**: 事件监听器需要处理重复触发（如重复审批事件）

## 策略模式：还款计算器

还款计算采用 **策略模式**，每种还款方式独立实现，通过注册表统一管理。

### 架构

```
RepaymentCalculator (接口)
  ├── EqualInstallmentCalculator  (等额本息)
  ├── EqualPrincipalCalculator    (等额本金)
  ├── InterestOnlyCalculator     (先息后本)
  ├── BalloonCalculator          (气球贷)
  └── DueOneTimeCalculator       (到期一次性还本付息)

RepaymentCalculatorRegistry (注册表)
  - 自动收集所有 RepaymentCalculator Bean
  - 按 RepaymentMethod 枚举建立映射
  - 提供 getCalculator(method) 查找
```

### 调用方

- **RepaymentScheduleServiceImpl**: 生成还款计划时，通过注册表获取计算器，循环调用 `calculate()` 填充每期数据
- **LoanApplicationServiceImpl**: 审批通过时，通过注册表获取等额本息计算器计算月供

### 新增还款方式

只需添加一个实现 `RepaymentCalculator` 的 `@Component` 类，无需修改任何现有代码。

## 规约模式：客户验证链

客户创建时的验证采用 **规约模式**，每条验证规则独立实现，通过 `and()` 组合成验证链。

### 架构

```
CustomerSpecification (接口)
  ├── MonthlyIncomeSpecification          (月收入不能为负数)
  ├── EmergencyContactPhoneSpecification  (紧急联系人电话格式)
  ├── IdCardUniquenessSpecification       (身份证号唯一性)
  ├── PhoneUniquenessSpecification        (手机号唯一性)
  ├── AgeSpecification                    (年龄 18-65 岁)
  └── BlacklistSpecification              (黑名单检查)

SpecificationResult (record)
  - satisfied: boolean
  - message: String (失败原因)
```

### 调用方

- **CustomerServiceImpl**: 构造函数注入所有 `CustomerSpecification` Bean，通过 `reduce(CustomerSpecification::and)` 组合为单一验证链，在 `createCustomer()` 中执行

### 新增验证规则

只需添加一个实现 `CustomerSpecification` 的 `@Component` 类，Spring 自动收集并加入验证链，无需修改 `CustomerServiceImpl`。

## 领域事件架构

事件模块采用 **分层路由** 模式，将事件定义、路由、业务处理三层分离。

### 架构

```
event/
  DomainEvent              (抽象基类 — 事件 ID、时间戳)
  DomainEventHandler<T>    (处理器接口 — handle + supportsEventType)
  EventBus                 (事件总线 — 封装 Spring ApplicationEventPublisher)
  DomainEventWrapper       (Spring 事件包装器)
  ApplicationApprovedEvent (审批通过事件)
  ContractCreatedEvent     (合同创建事件)
  handler/
    ApplicationApprovedHandler  (审批通过 → 创建合同)
    ContractCreatedHandler      (合同创建 → 生成还款计划)

listener/
  ApplicationApprovedEventListener  (路由 → ApplicationApprovedHandler)
  ContractCreatedEventListener      (路由 → ContractCreatedHandler)
```

### 职责分离

| 层 | 职责 | 示例 |
|----|------|------|
| **事件类** | 定义事件数据结构 | `ApplicationApprovedEvent` |
| **事件总线** | 统一发布入口，包装为 Spring 事件 | `EventBus.publish()` |
| **监听器** | 路由事件到处理器 | `ApplicationApprovedEventListener` |
| **处理器** | 执行业务逻辑 | `ApplicationApprovedHandler` |

### 事件发布

发布者通过 `EventBus` 发布领域事件，不再直接操作 Spring 的 `ApplicationEventPublisher`：

```java
// 之前
eventPublisher.publishEvent(new ApplicationApprovedEvent(this, appId, ...));

// 之后
eventBus.publish(new ApplicationApprovedEvent(appId, ...));
```

### 新增事件

1. 创建事件类继承 `DomainEvent`
2. 创建处理器实现 `DomainEventHandler<T>`
3. 创建监听器注入处理器，使用 `@TransactionalEventListener`
4. 在发布者中通过 `EventBus.publish()` 发布

## DTO 映射器模式

DTO 转换逻辑从 DTO 类和 Controller 中抽离到独立的 **Mapper** 层，通过统一的 `EntityMapper` 接口管理。

### 架构

```
dto/mapper/
  EntityMapper<E, D>          (接口 — toDto / toEntity)
  CustomerMapper              (Customer → CustomerResponse)
  LoanApplicationMapper       (LoanApplication → LoanApplicationResponse)
  ContractMapper              (LoanContract → ContractResponse)
  LoanProductMapper           (LoanProduct → LoanProductResponse)
  RepaymentScheduleMapper     (RepaymentSchedule → RepaymentScheduleResponse)
```

### 职责分离

| 层 | 职责 | 示例 |
|----|------|------|
| **DTO 类** | 纯数据容器，不含转换逻辑 | `CustomerResponse` |
| **Mapper** | 实体 ↔ DTO 转换 | `CustomerMapper.toDto(entity)` |
| **Controller** | 注入 Mapper，调用 `toDto()` | `customerMapper.toDto(saved)` |

### 使用方式

```java
// Controller 中注入 Mapper
@Autowired
private CustomerMapper customerMapper;

// 转换实体为响应 DTO
return ResponseEntity.ok(ApiResponse.success(customerMapper.toDto(saved)));

// 分页转换
Page<CustomerResponse> respPage = page.map(customerMapper::toDto);
```

### 新增 Mapper

1. 创建类实现 `EntityMapper<E, D>`
2. 标注 `@Component`
3. 在 Controller 中 `@Autowired` 使用

## 前端 API 层架构

前端 API 层采用 **分层封装** 模式，将 HTTP 客户端、响应解包、业务 API 三层分离。

### 架构

```
api/
  client.ts          (axios 实例 — 拦截器、Token 刷新)
  types.ts           (通用类型 — ApiResponse<T>、PageData<T>、ApiError)
  request.ts         (类型安全封装 — unwrap<T>() 自动解包 ApiResponse)
  auth.ts            (认证 API)
  customers.ts       (客户 API)
  applications.ts    (贷款申请 API)
  contracts.ts       (合同 API)
  products.ts        (产品 API)
  dashboard.ts       (仪表盘 API)
```

### 职责分离

| 层 | 职责 | 示例 |
|----|------|------|
| **client.ts** | axios 实例、请求/响应拦截器、Token 自动附加与刷新 | `apiClient.interceptors.request.use(...)` |
| **types.ts** | 通用类型定义 | `ApiResponse<T>`、`PageData<T>`、`ApiError` |
| **request.ts** | 类型安全封装，自动解包 `ApiResponse<T>` 中的 `data`，非零 code 抛 `ApiError` | `unwrap<T>()`、`pageGet<T>()` |
| **业务 API** | 各模块的 API 方法，返回已解包的类型 | `customerApi.list()` 返回 `Promise<PageData<Customer>>` |

### 使用方式

```typescript
// 页面中直接使用已解包的数据
const { data } = useQuery({
  queryKey: ["customers", search],
  queryFn: () => customerApi.list({ keyword: search, page: 0, size: 100 }),
});
// data 类型为 PageData<Customer>，可直接访问 data.content
const customers = data?.content ?? [];
```

### 错误处理

- 业务错误（`code !== 0`）在 `request.ts` 中统一抛出 `ApiError`
- 页面通过 `onError` 回调捕获并展示错误信息
- 401 未授权在 `client.ts` 拦截器中自动尝试 Token 刷新

## 审计日志架构

审计日志采用 **声明式注解 + AOP 切面** 模式，将审计逻辑从业务代码中完全抽离。

### 架构

```
audit/
  AuditLog              (实体 — 审计日志记录)
  AuditLogRepository    (仓库)
  SecurityUtil          (工具 — 获取当前用户、客户端 IP)
  AuditLoggable         (注解 — 声明需要审计的方法)
  RequireAdmin          (注解 — 声明需要管理员权限的方法)
  AuditAspect           (切面 — 拦截 @AuditLoggable 自动记录日志)
  SecurityAspect        (切面 — 拦截 @RequireAdmin 验证权限)
```

### 职责分离

| 层 | 职责 | 示例 |
|----|------|------|
| **@AuditLoggable** | 声明式标记，描述操作类型、实体类型、操作内容 | `@AuditLoggable(operation = "CREATE_CUSTOMER", ...)` |
| **@RequireAdmin** | 声明式标记，方法仅允许管理员调用 | `@RequireAdmin` |
| **AuditAspect** | 自动拦截注解方法，执行前后记录审计日志 | `@Around("@annotation(auditLoggable)")` |
| **SecurityAspect** | 自动拦截注解方法，验证当前用户角色 | `@Around("@annotation(requireAdmin)")` |

### 使用方式

```java
// 业务方法只需添加注解，无需手动调用 auditLog()
@Override
@AuditLoggable(operation = "CREATE_CUSTOMER", entityType = "Customer",
        description = "创建客户 {0.name}")
public Customer createCustomer(Customer customer, List<MultipartFile> documents) {
    // 纯业务逻辑，无审计代码
}

// 需要管理员权限的方法
@Override
@RequireAdmin
@AuditLoggable(operation = "BATCH_UPDATE_CUSTOMER_STATUS", ...)
public void batchUpdateCustomerStatus(...) {
    // 纯业务逻辑
}
```

### 注解参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| `operation` | String | 操作标识，如 `CREATE_CUSTOMER` |
| `entityType` | String | 实体类型，如 `Customer` |
| `description` | String | 操作描述，支持 `{0}`、`{1}` 占位符引用方法参数 |

### 设计原则

1. **声明式优于命令式**: 通过注解声明审计需求，而非在业务代码中手动调用 `auditLog()`
2. **关注点分离**: 审计逻辑完全在切面中实现，业务代码零侵入
3. **自动参数提取**: 切面自动从方法参数中提取实体 ID（第一个 Long 参数）和构建描述
4. **失败记录**: 方法抛出异常时，切面自动记录失败日志（含错误信息）
5. **权限与审计分离**: `@RequireAdmin` 和 `@AuditLoggable` 是两个独立注解，可组合使用
