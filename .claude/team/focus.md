---
current_app: tagManager
phase: development
start_date: 2026-06-14
sprint_duration_days: 14
active_roles:
  - product-manager
  - frontend-dev
  - backend-dev
  - test-engineer
standby_roles:
  - tech-architect
  - devops-engineer
next_apps:
  - knowledgeBase
  - aiChat
  - auth
---

## 当前迭代：tagManager

> **目标**：完善标签管理模块的功能和稳定性
> **截止**：2026-06-28

### 本轮聚焦范围
- ✅ 只开发 tagManager 模块的新功能和 bug 修复
- ❌ knowledgeBase、aiChat、taskManager 等其他模块暂不开发（只修 critical bug）
- 📋 其他模块的需求可以记录到 task-board backlog，但暂不实施

### 角色激活状态
| 角色 | 状态 | 说明 |
|------|------|------|
| product-manager | 🟢 活跃 | 聚焦 tagManager 需求分析和验收 |
| frontend-dev | 🟢 活跃 | 聚焦 tagManager 前端开发 |
| backend-dev | 🟢 活跃 | 聚焦 tagManager 后端开发 |
| test-engineer | 🟢 活跃 | 聚焦 tagManager 测试 |
| tech-architect | 🟡 待命 | 按需介入 tagManager 架构问题 |
| devops-engineer | 🟡 待命 | 发布阶段激活 |

### 切换应用流程
当需要切换到下一个应用时（如 knowledgeBase）：
1. 更新本文件的 `current_app`、`start_date`、`phase` 字段
2. 调整 `active_roles` 和 `standby_roles`
3. 更新 `next_apps` 列表
4. 在 task-board.md 中归档当前迭代的已完成任务
5. 所有 agent 下一轮工作前会读取本文件，自动对齐到新应用
