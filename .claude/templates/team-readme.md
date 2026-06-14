# AI 工程团队 — 新项目设置指南

## 快速开始（5 分钟）

### 1. 复制团队文件到新项目

```bash
# 从 superbox 复制团队结构（或从本 templates 目录所在的 .claude 目录）
cp -r .claude/agents <新项目>/.claude/agents
cp -r .claude/team <新项目>/.claude/team
cp -r .claude/templates <新项目>/.claude/templates
mkdir -p <新项目>/test/issue/app
```

### 2. 修改技术栈信息

编辑每个 `agents/*.md` 文件，将「技术栈」和「项目结构」部分替换为新项目的实际情况。其余内容（职责、用法、协作流程）无需修改。

### 3. 配置当前迭代

编辑 `team/focus.md`：
- 修改 `current_app` 为第一个要开发的应用模块
- 修改 `start_date` 和 `sprint_duration_days`
- 调整 `next_apps` 列表

### 4. 更新 CLAUDE.md

在新项目的 `CLAUDE.md` 中追加：

```markdown
## AI Engineering Team

本项目使用多角色 AI 工程团队。通过 `@agent-name` 调用团队成员。

### 团队成员
- `@tech-architect` — 技术选型、架构设计、API 契约、数据建模
- `@product-manager` — 功能设计、用户故事、需求优先级
- `@frontend-dev` — 前端开发和 bug 修复
- `@backend-dev` — 后端开发和 bug 修复
- `@test-engineer` — 功能测试、bug 报告、修复验证
- `@devops-engineer` — 版本发布、部署、运行监控

### 协作文件
- 任务看板：`.claude/team/task-board.md`
- 当前聚焦：`.claude/team/focus.md`
- 发布记录：`.claude/team/release-notes.md`

### 工作流
1. 检查 `team/focus.md` 确认当前聚焦应用
2. 产品经理编写任务 → task-board.md
3. 架构师审核（按需）→ PLAN.md
4. 前端/后端实现 → 代码变更
5. 测试工程师验证 → test/issue/ 文件
6. 运维工程师发布 → release-notes.md
```

### 5. 配置权限

在 `.claude/settings.local.json` 中添加 agent 所需的 Bash 工具权限（如 `Bash(mvn:*)`、`Bash(npm:*)`、`Bash(docker:*)` 等），以及 `.claude/` 和 `test/` 目录的写入权限。

### 6. 验证

```
@tech-architect review the current project structure
```

如果 agent 正常响应，团队已就绪。

## 自定义角色

1. 复制 `templates/agent-template.md`
2. 修改 frontmatter（name、description、tools）
3. 填充角色定位、职责、用法说明
4. 放入 `.claude/agents/` 目录
5. 在 CLAUDE.md 和 team-config.md 中注册新角色

## 目录结构

```
.claude/
  agents/              # Agent 角色定义（每个 agent 一个 .md 文件）
    ...
  team/                # 团队协作文件
    focus.md           # 当前迭代聚焦
    task-board.md      # 任务看板
    team-config.md     # 团队配置
    release-notes.md   # 发布记录
  templates/           # 跨项目复用模板
    agent-template.md
    team-readme.md     # 本文件
  settings.local.json  # 权限配置
  scheduled_tasks.json # 定时任务
```
