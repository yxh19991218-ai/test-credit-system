# ADR-0001: 采用事件驱动架构解耦审批与履约聚合

**日期**: 2026-04-30  
**状态**: 已采纳  
**涉及人**: 架构师、业务分析师

## 问题描述

当前代码中，`LoanApplicationService.approveToContract()` 方法直接处理"申请审批通过 → 生成合同 → 生成还款计划"的整个流程。这导致：

1. **聚合职责混乱**: 审批聚合（LoanApplication）掌管了履约聚合（LoanContract）的创建
2. **双向关联**: LoanApplication 和 LoanContract 互相引用（`contractId` 和 `applicationId`），边界模糊
3. **测试困难**: 需要模拟合同和还款计划，且流程路径不清晰
4. **扩展受限**: 若未来需要"审批通过后延迟创建合同"等流程变化，难以支持

## 决策

采用**事件驱动架构**，将流程改为：

```
审批通过 → 发布事件 → 合同聚合监听创建 → 发布事件 → 还款聚合监听生成计划
```

### 具体设计

1. **移除反向关联**: LoanApplication 不再持有 `contractId`
2. **单向引用**: LoanContract 仅通过 `applicationId` 引用 LoanApplication
3. **发布事件**:
   - `ApplicationApprovedEvent`: 审批聚合发布，携带审批结果（金额、期限、利率等）
   - `ContractCreatedEvent`: 履约聚合发布，用于还款聚合订阅
4. **事务边界**: 事件在事务提交后发布（TransactionPhase.AFTER_COMMIT）

## 影响

### 优势
- ✅ **清晰边界**: 三个聚合各司其职，通过事件异步协调
- ✅ **易于测试**: 每个聚合独立测试，事件监听器单独测试
- ✅ **灵活扩展**: 可轻松支持审批后人工审核、延迟放款等流程变化
- ✅ **可观察性**: 事件流清晰，便于追踪和调试

### 风险与缓解
| 风险 | 缓解方案 |
|------|---------|
| 事件丢失 | 使用 TransactionPhase.AFTER_COMMIT，确保数据先入库再发事件 |
| 重复创建合同 | 合同创建时检查 applicationId 唯一性，实现幂等性 |
| 系统间不一致 | 事件驱动当前仅在单体内，暂不涉及跨系统问题 |

## 代码变更

### 删除
- `LoanApplicationService.approveToContract()`
- `LoanApplication.contractId` 字段
- `LoanApplicationController` 中的 `/to-contract` 端点

### 新增
- `ApplicationApprovedEvent`: 申请审批通过事件
- `ContractCreatedEvent`: 合同已创建事件
- `ApplicationApprovedEventListener`: 监听并创建合同
- `ContractCreatedEventListener`: 监听并生成还款计划

### 修改
- `ContractService.createContractFromApplication()`: 新增方法，从事件监听器调用
- `LoanApplicationServiceImpl.reviewApplication()`: 审批通过时发布事件

## 测试策略

1. **单元测试**: 
   - 审批聚合：验证 reviewApplication 时正确发布事件
   - 合同聚合：验证 createContractFromApplication 正确创建合同
   - 还款聚合：验证生成计划逻辑

2. **集成测试**:
   - 完整的"审批 → 合同 → 还款计划"流程

## 相关决策
- 见 CONTEXT.md 中的"聚合边界"和"事件驱动流程"
