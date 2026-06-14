# Superbox 工程团队配置

## 团队组成

| 角色 | Agent 名称 | 文件 | 模型 | 状态 |
|------|-----------|------|------|------|
| 技术架构师 | `@tech-architect` | agents/architect.md | sonnet | 🟡 待命 |
| 产品经理 | `@product-manager` | agents/product-manager.md | sonnet | 🟢 活跃 |
| 前端开发工程师 | `@frontend-dev` | agents/frontend-dev.md | sonnet | 🟢 活跃 |
| 后端开发工程师 | `@backend-dev` | agents/backend-dev.md | sonnet | 🟢 活跃 |
| 测试工程师 | `@test-engineer` | agents/test-engineer.md | sonnet | 🟢 活跃 |
| 运维工程师 | `@devops-engineer` | agents/devops-engineer.md | sonnet | 🟡 待命 |

## 协作文件

| 文件 | 用途 |
|------|------|
| `team/focus.md` | 当前迭代聚焦的应用和阶段 |
| `team/task-board.md` | 共享任务看板 |
| `team/release-notes.md` | 版本发布记录 |
| `team/team-config.md` | 本文件，团队配置 |

## 迭代模型

- **模式**：单应用聚焦，逐个应用迭代
- **节奏**：需求 → 开发 → 测试 → 发布 → 切换
- **聚焦规则**：非当前应用只修 critical bug，功能需求进入 backlog

## 自动化

| Cron Job | 频率 | 用途 |
|----------|------|------|
| `49d4189d` | 每 2 分钟 | 自动修复 submit issue |
| `ed390491` | 每 17 分钟 | 自动验证 retest issue |
| 待添加 | 每 30 分钟 | 任务看板监控 |
| 待添加 | 每天 9:37 | 发布就绪检查 |
| 待添加 | 每天 18:43 | 测试覆盖报告 |
| 待添加 | 每 2 小时 17 分 | 跨 agent 同步检查 |
