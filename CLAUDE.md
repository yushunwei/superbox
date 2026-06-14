## 项目概述

日常工具箱项目 — 日常工作中需要经常使用的工具箱，如：任务管理、记事本、文本翻译、个人知识库等。需要支持多语言（中文和英文）。

## 技术栈

- 前端: Vue 3 + Vite + Pinia + Vue Router 4 + Element Plus + TypeScript
- 后端: springboot + mybatis + redis
- 数据库: PostgreSQL
- PDF: pdfkit

## 开发规范
- 后端：使用阿里巴巴Java开发手册黄山版

# 行为准则

减少常见 LLM 编码错误的行为准则。根据需要与项目特定说明合并使用。

**权衡：** 这些准则偏向谨慎而非速度。对于简单任务，可自行判断。

## 1. 先思考，再编码

**不要假设。不要隐藏困惑。明确列出权衡。**

实施之前：
- 明确陈述你的假设。如果不确定，请提问。
- 如果存在多种理解方式，把它们列出来——不要默默选一个。
- 如果有更简单的方法，指出来。必要时可以反驳。
- 如果有不清楚的地方，停下来。说清楚哪里困惑。然后提问。

## 2. 简单至上

**用最少的代码解决问题。不做投机性开发。**

- 不添加需求之外的功能。
- 不为单一用途的代码创建抽象。
- 不添加未要求的"灵活性"或"可配置性"。
- 不为不可能发生的场景添加错误处理。
- 如果写了 200 行但 50 行就能搞定，重写。

问自己："高级工程师会觉得这过度复杂吗？"如果是，就简化。

## 3. 精准修改

**只改必须改的。只清理自己弄乱的。**

编辑现有代码时：
- 不要"改进"无关的代码、注释或格式。
- 不要重构没坏的东西。
- 匹配现有风格，即使你本来会用不同的写法。
- 如果注意到无关的死代码，提出来——但别删。

当你的改动导致孤立代码时：
- 删除因你的改动而变得无用的导入/变量/函数。
- 不要删除之前就存在的死代码，除非被明确要求。

检验标准：每一处改动都应该能追溯到用户的具体需求。

## 4. 目标驱动执行

**定义成功标准。循环迭代直到验证通过。**

将任务转化为可验证的目标：
- "添加验证" → "先为无效输入编写测试，然后让测试通过"
- "修复 bug" → "先写一个能复现 bug 的测试，然后修复"
- "重构 X" → "确保重构前后测试全部通过"

对于多步骤任务，给出简要计划：
```
1. [步骤] → 验证: [检查项]
2. [步骤] → 验证: [检查项]
3. [步骤] → 验证: [检查项]
```

清晰的验收标准让你能够独立迭代。模糊的标准（"把它做好"）则需要不断追问。

**这些准则有效的标志是：** diff 中不必要的改动变少，因过度复杂导致的重写变少，澄清性问题在实施之前提出而不是在犯错之后。

---

## AI Engineering Team

本项目使用多角色 AI 工程团队协作开发，团队定义在 `.claude/agents/` 目录。通过 `@agent-name` 调用团队成员。

### 团队成员

| 角色 | 调用方式 | 职责 |
|------|---------|------|
| 技术架构师 | `@tech-architect` | 技术选型、架构设计、API 契约、数据建模、架构级 review |
| 产品经理 | `@product-manager` | 功能设计、用户故事、需求优先级、范围控制 |
| 前端开发工程师 | `@frontend-dev` | Vue 3/uni-app 组件开发、前端 bug 修复、状态管理 |
| 后端开发工程师 | `@backend-dev` | Spring Boot 开发、API 实现、数据库迁移、后端 bug 修复 |
| 测试工程师 | `@test-engineer` | 功能测试、bug 报告与验证、测试用例设计 |
| 运维工程师 | `@devops-engineer` | 版本发布、Docker 部署、CI/CD、运行监控 |

每个 agent 文件的「用法说明」章节包含详细的调用场景和示例。

### 迭代模式

本项目采用**单应用聚焦**开发模式——每个时期团队聚焦一个应用模块（如 taskManager），逐应用迭代。当前聚焦应用见 `.claude/team/focus.md`。

非聚焦应用只修 critical bug，功能需求进入 `.claude/team/task-board.md` backlog。

**重要：** 每次处理任务前，必须重新读取 `.claude/team/focus.md` 获取最新聚焦应用，不得依赖之前缓存的读取结果。聚焦应用可能随时切换。

### 协作文件

| 文件 | 用途 |
|------|------|
| `.claude/team/focus.md` | 当前迭代聚焦的应用和阶段 |
| `.claude/team/task-board.md` | 共享任务看板 |
| `.claude/team/release-notes.md` | 版本发布记录 |
| `.claude/team/team-config.md` | 团队配置总览 |
| `.claude/templates/team-readme.md` | 新项目复用指南 |

### 工作流

1. 确认 `.claude/team/focus.md` 当前聚焦应用
2. 产品经理编写任务 → task-board.md
3. 架构师审核（按需） → PLAN.md
4. 前端/后端实现 → 代码变更
5. 测试工程师验证 → `test/issue/` 文件
6. 运维工程师发布 → release-notes.md
---

## Git 分支与提交规范

### 分支模型

| 分支 | 用途 | 备注 |
|------|------|------|
| `master` | 生产就绪代码，每次 sprint 结束合并 + 打 tag | 不直接提交 |
| `develop` | 当前 sprint 集成开发 | 日常开发基分支 |
| `feature/<缩写>-<描述>` | 单个功能/修复分支 | 合并后删除 |
| `hotfix/<缩写>-<描述>` | 生产紧急修复 | 合并到 master 和 develop |

模块缩写：`tm`=taskManager、`tg`=tagManager、`kb`=knowledgeBase、`ai`=aiChat、`au`=auth、`infra`=infrastructure

### Commit 格式

```
<type>(<scope>): <subject>

type:  feat / fix / docs / refactor / style / test / chore
scope: tm / tg / kb / ai / au / infra / team
```

### 日常工作流

开发 → `git checkout -b feature/<scope>-<desc>` from develop → 提交 → 合并回 develop → push

Sprint 发布 → `git checkout master && git merge develop && git tag -a v<version> && git push origin master --tags`

**所有 AI agent 创建 commit 时必须遵循上述格式。**
