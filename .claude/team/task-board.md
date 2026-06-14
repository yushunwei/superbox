# Superbox Engineering Team — Task Board

> 当前聚焦应用：**tagManager**（见 `focus.md`）
> 最后更新：2026-06-14

---

## Backlog（待规划）

> 非当前聚焦应用的功能需求暂存于此，等待对应迭代再拆分实施。

<!-- 新任务格式：
### TASK-{NNN}
---
id: TASK-NNN
title: "简要标题"
status: backlog
assignee: frontend-dev | backend-dev | both
priority: high | medium | low
module: taskManager | knowledgeBase | aiChat | auth | tagManager | infrastructure
type: feature | enhancement | bugfix | refactor
description: |
  详细描述，含验收条件。
---
-->

---

## Ready（就绪待开发）

> 已通过需求评审和架构评审，可以开始开发的任务。

### TASK-010
---
id: TASK-010
title: "标签拖拽排序"
status: ready
assignee: both
priority: medium
module: tagManager
type: enhancement
description: |
  标签列表支持拖拽排序，sortOrder 持久化到数据库。
  验收条件：
  - 前端标签列表可拖拽调整顺序
  - 后端 sortOrder 更新接口正常
  - 排序结果持久化
---

### TASK-011
---
id: TASK-011
title: "标签批量操作"
status: ready
assignee: both
priority: medium
module: tagManager
type: enhancement
description: |
  支持标签的批量删除和批量修改分类。
  验收条件：
  - 多选标签后显示批量操作工具栏
  - 批量删除有确认弹窗
  - 批量移动分类功能正常
---

### TASK-012
---
id: TASK-012
title: "分类拖拽排序"
status: ready
assignee: both
priority: low
module: tagManager
type: enhancement
description: |
  左侧分类列表支持拖拽排序，sortOrder 持久化。
  验收条件：
  - 分类列表可拖拽调整顺序
  - 排序结果持久化
---

## In Progress（开发中）

> 当前正在执行的任务。

### TASK-013
---
id: TASK-013
title: "tagManager 模块现状审查与审计"
status: in_progress
assignee: all
priority: high
module: tagManager
type: review
description: |
  全面审查 tagManager 模块现有代码质量、功能完整度和潜在问题。
  验收条件：
  - 前端 API 封装完整度检查
  - 后端接口功能验证
  - UI/UX 可用性评估
  - 产出问题清单和改进建议
---

---

## Review（待审查）

> 开发完成，等待测试验证。

---

## Done（本轮已完成）

> 本轮迭代已验收通过的任务。

---

## 已完成迭代归档

### Sprint 1 — taskManager 功能完善（2026-06-14）

| 任务 | 类型 | 状态 |
|------|------|------|
| 导入/导出对话框布局与功能修复（按钮对齐、模板下载403、文件上传响应） | bugfix | ✅ done |
| 搜索栏位置修复（position: fixed 居中布局） | bugfix | ✅ done |
| 导入对话框布局修复（label 行内元素导致错乱） | bugfix | ✅ done |
| 列过滤面板多选改造（radio → checkbox + 确定/取消） | enhancement | ✅ done |
| 导出支持筛选参数（仅导出当前筛选结果） | enhancement | ✅ done |

### Sprint 0 — 团队基础设施搭建（2026-06-14）

### TASK-001
---
id: TASK-001
title: "搭建 AI 工程团队基础设施"
status: done
assignee: all
priority: high
module: infrastructure
type: setup
description: |
  创建 .claude/agents/（6个角色定义）、.claude/team/（focus.md、task-board.md、release-notes.md、team-config.md）、.claude/templates/（复用模板）
  验收条件：
  - 6个 agent 文件创建完毕，可通过 @agent-name 调用
  - focus.md 标识当前迭代聚焦应用
  - task-board.md 可作为协作中枢
---
