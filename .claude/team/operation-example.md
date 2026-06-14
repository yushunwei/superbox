# Superbox 团队协作操作示例

> 本文档演示用户只需描述「做什么」，6 个 AI 角色如何自主协同完成任务。
> IDE：Visual Studio Code + Claude Code 扩展

---

## 核心理念

**用户只管「做什么」，团队负责「怎么做」。**

用户不需要指定 API 路径、数据库字段、组件名称。这些由技术架构师和开发工程师在协同中自主决定。

---

## 前置条件

- VS Code 已安装 Claude Code 扩展
- 终端在项目根目录 `d:\project\superbox\`
- `.claude/agents/` 下 6 个角色定义已就绪

---

## 四种场景

| | A：功能开发 | B：线上 Bug 修复 | C：单模块完整测试 | D：全工程回归测试 |
|---|---|---|---|---|
| 触发 | 提新功能需求 | 报告异常现象 | 对某模块做全面体检 | 大版本发布前全量检查 |
| 涉及角色 | 全部 6 个 | 开发+测试+(按需)运维 | 测试+开发+运维 | 测试+开发+运维 |
| 流程长度 | 数小时~数天 | 数分钟 | 数十分钟~数小时 | 数小时 |
| 走 task-board | ✅ | ❌ | ❌ | ❌ |
| 走产品经理 | ✅ | ❌ | ❌ | ❌ |
| 走架构师 | ✅ | ❌ | ❌ | ❌ |
| 发布 | 常规版本发布 | 修复跟随下次发布或 hotfix | bug 修复后可选择发布 | bug 修复后通常发布 |

---

# 场景 A：常规功能开发

> 适用：新增功能模块、大功能改造、需要产品经理提前设计的需求

## A.0 启动迭代

编辑 `.claude/team/focus.md`，修改 `current_app`：

```markdown
current_app: knowledgeBase
phase: design
```

`Ctrl+S` 保存。所有 agent 下一轮工作前自动读取此文件。

---

## A.1 提需求（用户 → 产品经理）

### 用户说一句话

```
@product-manager 知识库模块需要支持：文件上传（PDF/Word/文本）、在线预览、
全文搜索、以及文件夹分类管理。
```

不需要指定接口、字段、UI 布局。

### 角色自主协同

**产品经理** 会：

1. 阅读现有知识库模块代码，了解已有能力
2. 将需求拆解为用户故事，每个附验收条件
3. 按优先级排序，写入 `task-board.md`

**产出示例**（task-board.md 中自动生成）：

```
### TASK-002
id: TASK-002
title: "文件上传（PDF/DOCX/TXT/MD）"
status: backlog
priority: high
module: knowledgeBase
type: feature
description: |
  用户可以将本地文件上传到知识库。
  验收条件：
  - 支持 PDF、DOCX、TXT、Markdown 四种格式
  - 上传后自动解析文本内容
  - 超过 50MB 的文件给出提示
  - 上传进度实时显示

### TASK-003
id: TASK-003
title: "知识条目列表与全文搜索"
status: backlog
...
```

用户打开 `task-board.md` 确认拆分是否符合预期。如有偏差：

```
@product-manager 全文搜索还需要支持按标签筛选，补充到 TASK-003 中
```

---

## A.2 架构设计（产品经理 → 架构师）

### 用户无需介入，角色自主触发

**方式一 — cron 自动触发**（30 分钟）：`orchestrator-task-board` job 自动扫描 backlog 高优任务，以架构师角色评审，标记 ready。

**方式二 — 用户手动触发**：

```
@tech-architect 看一下 task-board 中 knowledgeBase 的任务，确认架构是否合理
```

### 角色自主协同

**架构师** 会：

1. 读取 task-board 中 knowledgeBase 的任务
2. 检查与现有架构的兼容性
3. 设计 API 契约（路径、请求/响应结构）和数据模型（表结构、索引）
4. 将架构决策写入任务描述中，标记 status 为 `ready`
5. 重大变更同步更新 `PLAN.md`

---

## A.3 开发实现（架构师 → 前后端开发）

### 用户说一句话启动

```
@frontend-dev 开始开发 task-board 中 knowledgeBase 的 ready 任务
@backend-dev 开始开发 task-board 中 knowledgeBase 的 ready 任务
```

### 角色自主协同

**后端**：读 task-board → 读 API 契约 → 读现有代码风格 → Flyway 迁移 → Entity → Mapper → Service → Controller → 标记 `review`

**前端**：读 task-board → 读 API 契约（不等后端写完）→ 读现有组件风格 → API 模块 → Store → 组件/页面 → i18n 双语更新 → 标记 `review`

**前后端发现契约模糊时自动协调**，用户不需要介入。

### 查看进度

随时打开 `task-board.md`：
```
backlog → ready → in_progress → review → done
```

---

## A.4 测试验证（开发 → 测试工程师）

### 用户说一句话

```
@test-engineer 对 knowledgeBase 模块进行功能测试
```

### 角色自主协同

**测试工程师** 会：

1. 读 task-board 中已完成的开发任务
2. 读产品经理的验收条件
3. 读前端组件和后端 Controller 源码，识别边界
4. 在 `test/issue/app/knowledgeBase.md` 中编写测试用例
5. 发现 bug 以 `IS-NNN` 格式记录，status 为 `submit`

**bug 自动修复**（无需用户参与）：

```
submit →（2min cron 自动修复）→ retest →（17min cron 自动验证）→ fixed
                                 ↓ 修复不通过
                              退回 submit（附说明）
```

查看测试结果：

```
@test-engineer 输出 knowledgeBase 模块的测试报告
```

---

## A.5 发布（测试 → 运维工程师）

### 用户说一句话

```
@devops-engineer 准备发布 knowledgeBase 的迭代成果
```

### 角色自主协同

**运维工程师** 自动：检查 task-board 完成情况 → 检查 critical/major issue → `mvn clean package` + `npm run build` → Docker 构建验证 → 更新 `release-notes.md`。

有阻塞项会列出清单告知用户。版本号由运维工程师按变更内容自动判定，也可指定：

```
@devops-engineer 发布 v0.3.0
```

---

## A.6 复盘与切换

```
复盘 knowledgeBase 本轮迭代，然后切换到 aiChat
```

---

## 场景 A 总结：用户说了什么

| 阶段 | 用户只说 | 团队自主完成 |
|------|---------|-------------|
| 启动 | 改 `focus.md` | 所有 agent 自动对齐 |
| 需求 | `@product-manager 知识库需要文件上传、搜索、文件夹管理` | 拆解→验收条件→排优先级→task-board |
| 架构 | 等 cron 或 `@tech-architect 审核` | API 设计→数据建模→标记 ready |
| 开发 | `@frontend-dev / @backend-dev 开始开发` | 读契约→编码→自测→标记 review |
| 测试 | `@test-engineer 测试` | 用例→bug 报告→cron 修复→验证 |
| 发布 | `@devops-engineer 发布` | 检查→构建→部署→release-notes |
| 切换 | `切换到 aiChat` | 复盘→更新 focus.md |

---

# 场景 B：线上 Bug 修复

> 适用：已上线应用的异常行为、报错、数据错误、UI 故障等

## 核心理念：轻量化，不走完整流程

Bug 修复不需要产品经理参与（问题本身已明确了"哪里不对"），不需要架构师参与（修 bug 不应引入架构变更），不需要走 task-board（不属于功能迭代）。

```
用户描述异常现象
     ↓
Claude Code 自动判断归属（前端 or 后端 or 两端）
     ↓
开发工程师定位 → 修复 → 自测
     ↓
测试工程师验证
     ↓
（critical bug）运维工程师 hotfix 发布
```

---

## B.1 发现 Bug，一句话报告

### 方式一：交给团队自动处理（推荐）

直接描述现象，团队自主判断归属并修复：

```
知识库搜索框输入 % 号后页面白屏，控制台报 500 错误
```

Claude Code 会根据现象自动判断：
- 涉及「页面白屏」→ 前端需要参与
- 涉及「500 错误」→ 后端需要参与
- 自动协调两端同时排查

### 方式二：写入 issue 文件，让 cron 自动修复

在 `test/issue/app/knowledgeBase.md` 中追加：

```markdown
### Issue #IS-030
---
id: IS-030
title: "搜索特殊字符 % 导致页面白屏"
status: submit
severity: major
module: knowledgeBase
type: bug
found_by: user
---
**重现步骤：**
1. 打开知识库页面
2. 在搜索框输入 %
3. 页面白屏，控制台 500
**预期行为：**
特殊字符应正常搜索或给出提示，不应崩溃
```

保存后 2 分钟 cron job 自动拾取修复。适合不紧急的 bug。

---

## B.2 开发工程师自主修复

用户不需要指定是前端还是后端的问题。团队会根据 bug 现象自主判断：

- **纯前端问题**（UI 显示异常、交互错误）→ `@frontend-dev` 修复
- **纯后端问题**（500 错误、数据错误、接口超时）→ `@backend-dev` 修复
- **跨端问题**（数据对不上、接口联调不一致）→ 两端自动协调

如果用户能明确判断归属，也可以直接找对应角色加快速度：

```
@backend-dev 知识库搜索 % 报 500，帮我排查修复
```

### 修复过程

开发工程师会：
1. 定位相关源码
2. 分析根因
3. 实施修复
4. 自测验证
5. 写入 `test/issue/app/{模块}.md`，记录 issue 和修复方案

---

## B.3 测试工程师验证

修复完成后：

```
@test-engineer 验证 IS-030 的修复
```

测试工程师会重新阅读修复代码，确认 fix 正确，更新 issue 状态为 `fixed`。

如果 cron job 自动修复了该 issue，17 分钟验证 job 也会自动检查。

---

## B.4 发布（仅 critical bug 需要紧急发布）

### 普通 bug（major / minor）

修复合并到代码中，跟随下次常规迭代一起发布。用户无需额外操作。

### 紧急 bug（critical：线上崩溃、数据丢失、安全漏洞）

```
@devops-engineer IS-030 是线上崩溃级 bug，需要 hotfix 发布
```

运维工程师会跳过常规发布检查，直接：
1. 确认修复已合并
2. 快速构建
3. 部署上线
4. 发布后补充 release-notes

---

## 场景 B 总结：用户说了什么

| 紧急程度 | 用户操作 | 耗时 |
|---------|---------|------|
| 不紧急 | 写入 issue → cron 自动修复（2min 内） | ~2 分钟 |
| 普通 | 一句话描述 bug → 团队自动修复 → 验证 | ~10 分钟 |
| 紧急 | 一句话 → 开发修复 → `@devops-engineer hotfix` | ~30 分钟 |

用户最多说 2~3 句话，不需要开 task、不需要产品经理设计、不需要架构评审。

---

# 场景 C：单模块完整测试

> 适用：某个功能模块已开发完毕，需要对它做一次全面的功能测试和 bug 修复。
> 例如：「taskManager 开发告一段落，全面测一遍」「knowledgeBase 上线前做一次完整体检」

## 核心理念：以测试工程师为主导，开发工程师配合修复

不需要产品经理（不涉及新需求），不需要架构师（不涉及架构变更）。测试工程师系统性地扫描模块所有功能点，发现问题后开发工程师修复，测试再验证。

```
用户说「测一下 XX 模块」
     ↓
测试工程师：全面扫描 → 编写测试用例 → 发现 bug → 写入 issue
     ↓
开发工程师：认领 issue → 修复 → 标记 retest
     ↓
测试工程师：逐条验证 → 标记 fixed
     ↓
用户：查看测试报告，决定是否发布
```

---

## C.1 启动模块测试

### 用户一句话

```
@test-engineer 对 taskManager 模块做一次完整的测试
```

或者指定测试范围：

```
@test-engineer 对 taskManager 做完整测试，重点关注批量操作和 Excel 导入导出
```

---

## C.2 测试工程师自主执行（含测试调研）

**测试工程师** 接到指令后，**不会直接开始写用例**，而是先做系统性的测试调研：

### 调研阶段：从产品和开发两端获取上下文

**第一步 — 读产品需求**（理解「应该做什么」）：
1. `.claude/team/task-board.md` — 阅读当前聚焦应用的任务，提取用户故事和验收条件
2. 任务描述中的验收条件 — 这些决定「测什么」，是测试用例的直接来源

**第二步 — 读技术实现**（理解「怎么做」→ 找到边界）：
3. `PLAN.md` — 模块的架构设计、API 契约、数据模型
4. 后端 Controller 源码 — 所有 API 端点、参数、校验规则
5. 后端 Service 源码 — 业务逻辑、边界处理、validatePlanDates() 等校验方法
6. Flyway 迁移 SQL — 表结构、字段约束（NOT NULL、长度限制、唯一约束）
7. 前端页面组件 — 所有用户交互入口、事件处理、状态流转

**第三步 — 读历史记录**（防止回归漏测）：
8. `test/issue/app/{模块}.md` 历史文件 — 之前修过的 bug 不可复现
9. 已有 fixed issue 列表 — 每个都对应一个回归测试点

### 执行阶段：编写测试用例

基于调研结果，系统性地设计用例：

1. **对照验收条件** → 每个用户故事的正向测试（happy path）
2. **对照后端校验代码** → 边界条件测试（null、空串、超长、非法值）
3. **对照数据库约束** → 字段级别的完整性测试
4. **对照前端交互入口** → UI 操作的完整覆盖
5. **对照历史 issue** → 回归测试，确认不再复现
6. **跨端检查** → 前端展示的数据与后端 API 返回一致、小程序端 API 路径正确

### 测试产出示意

```
test/issue/app/taskManage.md 更新如下：

## 一、测试用例概览
| 用例编号 | 测试场景 | 预期结果 | 实际结果 |
| TC-CRUD-01 | 创建任务（正常数据） | 创建成功，列表刷新 | PASS |
| TC-CRUD-02 | 创建任务（空标题） | 提示"标题不能为空" | PASS |
| TC-CRUD-03 | 编辑任务标题 | 保存后标题更新 | PASS |
| TC-BATCH-01 | 批量删除 10 条任务 | 全部删除，列表刷新 | FAIL → IS-035 |
| TC-BATCH-02 | 批量排序（拖拽） | 新顺序持久化 | FAIL → IS-036 |
| TC-EXCEL-01 | 导出 Excel | 下载含所有字段的 xlsx | PASS |
| TC-EXCEL-02 | 导入 Excel（含错误行） | 提示错误行号，正确行导入 | FAIL → IS-037 |
...

## 二、问题详情
### Issue #IS-035
status: submit | severity: major | module: taskManager
...
```

---

## C.3 Bug 修复（三种模式：并行 / 自动 / 串行）

### 模式一：并行修复（推荐，最快）⚡

测试工程师开始测试后，随着 bug 不断被写入 issue 文件（status 为 `submit`），**立即开启并行会话让开发工程师同步修复**，不等测试全部完成。

**在 VS Code 中的操作：**

1. 打开第一个终端 → 启动测试：
   ```
   @test-engineer 对 taskManager 模块做一次完整的测试
   ```

2. `Ctrl+Shift+` 新开第二个终端 → 启动前端修复（后台）：
   ```
   @frontend-dev 持续监控 test/issue/app/taskManage.md，每发现一个 status 为 submit 且 assigned_to 为 frontend-dev 的 issue，立即定位修复，完成后标记 retest
   ```

3. 再新开第三个终端 → 启动后端修复（后台）：
   ```
   @backend-dev 持续监控 test/issue/app/taskManage.md，每发现一个 status 为 submit 且 assigned_to 为 backend-dev 的 issue，立即定位修复，完成后标记 retest
   ```

```
终端 1：@test-engineer          → 持续测试，不断产出 bug
终端 2：@frontend-dev（后台）    → 发现前端 bug → 立即修复 → 标记 retest
终端 3：@backend-dev（后台）     → 发现后端 bug → 立即修复 → 标记 retest
         ↑ 三线并行，互不阻塞 ↑
```

**实际效果**：测试工程师刚写完 IS-035（前端 bug），前端开发工程师在第二个终端就立即开始修复了；同时测试继续往下跑，不等待。

#### 前后端联动 Bug 如何处理

有些 bug 涉及前后端联动（如 API 契约不一致、数据流跨端问题），`assigned_to` 标记为 `both`。这类 bug 不适合两个独立终端各自修——会造成重复劳动或修复冲突。

**处理方式：对联动 bug，在主终端单会话协同修复**

```
@frontend-dev 和 @backend-dev 一起修复 IS-038，
这是前后端联动 bug：API 返回的字段名和前端期望的不一致，
请两端对齐后各自修改
```

单个会话中 Claude Code 会依次协调两端：
1. 先读 issue，理解联动的根因
2. 后端修改字段名或接口契约
3. 前端同步修改调用方
4. 两端一起验证

**判断规则**：

| issue 特征 | assigned_to | 用哪种方式 |
|-----------|-------------|-----------|
| 纯 UI 异常、组件渲染问题 | `frontend-dev` | 终端 2 独立修复 ✅ |
| 纯后端逻辑、500 错误、数据错误 | `backend-dev` | 终端 3 独立修复 ✅ |
| API 返回和前端展示不一致 | `both` | **主终端单会话协同** 🔄 |
| 新增功能需要前后端同时改动 | `both` | **主终端单会话协同** 🔄 |
| 跨端数据流问题（存进去对，拿出来错） | `both` | **主终端单会话协同** 🔄 |

**实际操作节奏**：

```
终端 1：@test-engineer               → 持续测试，产出 bug
终端 2：@frontend-dev（后台）         → 只修 assigned_to=frontend-dev 的单端 bug
终端 3：@backend-dev（后台）          → 只修 assigned_to=backend-dev 的单端 bug
         ↑ 三线并行，互不阻塞 ↑

等测试完成、单端 bug 修完后：
主终端：@frontend-dev 和 @backend-dev 一起修复 taskManage.md 中 assigned_to=both 的联动 bug
         ↑ 单线程收尾联动问题 ↑
```

这样就兼顾了「并行效率」和「联动准确性」。

### 模式二：自动修复（最省心）

不手动开启开发会话，靠 cron job 每 2 分钟自动扫描 `submit` issue 并修复。适合 bug 数量少或时间不急的情况。

### 模式三：串行修复（适合 bug 少）

等测试全部完成后，再一句话批量交给开发：

```
@frontend-dev 处理 taskManage.md 中所有 submit 的前端 issue
@backend-dev 处理 taskManage.md 中所有 submit 的后端 issue
```

---

## C.4 验证修复

修复完成后，用户一句话：

```
@test-engineer 验证 taskManage.md 中所有 retest 状态的 issue
```

测试工程师逐条复查修复代码，确认后标记 `fixed`。修复不完整的退回 `submit`。

---

## C.5 测试报告

```
@test-engineer 输出 taskManager 模块的测试报告
```

报告包含：总测试用例数、通过率、各模块 P/F 分布、遗留问题、质量评级。

---

## C.6 决定是否发布

测试报告出来后：

- **全部通过** → 可选择发布：`@devops-engineer 发布 taskManager 的测试修复`
- **有 minor 遗留** → 记录到 backlog，照常发布
- **有 major/critical 遗留** → 继续修复后再发布

---

## 场景 C 总结

| 步骤 | 用户只说 | 团队自主完成 |
|------|---------|-------------|
| 启动 | `@test-engineer 对 taskManager 做完整测试` | 全面扫描→用例→bug 报告 |
| 修复 | **并行**：另开终端 `@frontend-dev/@backend-dev 持续监控并修复` | 边测边修，互不阻塞 |
|  | **自动**：等 cron 每 2min 扫描修复 | 自动定位→修复→标记 retest |
|  | **串行**：测完后 `@frontend-dev/@backend-dev 处理 submit issue` | 逐个修复→标记 retest |
| 验证 | `@test-engineer 验证 retest issue` | 复查修复→标记 fixed/退回 |
| 报告 | `输出测试报告` | 测试统计+质量评级 |
| 发布 | `@devops-engineer 发布`（可选） | 构建→部署→release-notes |

---

# 场景 D：全工程回归测试

> 适用：大版本发布前、重大项目上线前、定期质量审计。
> 例如：「v1.0.0 发布前做一次全量回归」「每个月底做一次全工程质量检查」

## 核心理念：全模块、全流程、系统性扫描

这是场景 C 的扩展——不再是单个模块，而是系统性地覆盖所有已开发的应用模块（taskManager、knowledgeBase、aiChat、auth、tagManager 等）。目标是为发布决策提供完整的数据支撑。

---

## D.1 启动全工程测试

### 用户一句话

```
@test-engineer 对整个 superbox 工程做一次回归测试，所有模块全覆盖
```

或者指定本次回归侧重点：

```
@test-engineer 全工程回归测试，重点验证跨模块功能（任务转知识、标签筛选联动等）
```

---

## D.2 测试工程师自主执行

全工程回归的调研范围更大——需要对**每个模块**都执行场景 C 的调研流程。

**测试工程师** 接到指令后会：

1. **列出所有模块清单**：扫描 `superbox-frontend/src/apps/` 和 `superbox-backend/.../app/`
2. **逐模块调研**（与场景 C 相同）：读 task-board 验收条件 → 读 Controller/Service 源码 → 读 Flyway 迁移 → 读前端组件 → 读历史 issue
3. **按优先级排序测试**：核心模块优先 → 基础模块 → 跨模块交互
4. **重点关注回归项**：历史 fixed issue 不可复现、新增功能不影响已有功能、API 向后兼容
5. **汇总报告**：各模块 bug + 全工程通过率统计

### 产出示意

```
全工程回归测试 — 2026-06-28

| 模块 | 测试用例 | 通过 | 失败 | 状态 |
| taskManager | 45 | 42 | 3 | ⚠️ 3 issues |
| knowledgeBase | 38 | 35 | 3 | ⚠️ 3 issues |
| aiChat | 22 | 22 | 0 | ✅ 通过 |
| auth | 12 | 12 | 0 | ✅ 通过 |
| tagManager | 18 | 17 | 1 | ⚠️ 1 issue |
| 跨模块交互 | 8 | 6 | 2 | ⚠️ 2 issues |
| **合计** | **143** | **134** | **9** | **通过率 93.7%** |
```

---

## D.3 Bug 修复

全工程回归通常 bug 数量多、涉及模块广，**推荐使用并行模式**：

```
终端 1：@test-engineer           → 全工程回归测试，逐模块产出 bug
终端 2：@frontend-dev（后台）     → 持续监控所有模块的 submit issue，发现前端 bug 立即修复
终端 3：@backend-dev（后台）      → 持续监控所有模块的 submit issue，发现后端 bug 立即修复
```

cron 的 2 分钟自动修复 job 也会同步工作，相当于有 **3 条线同时修 bug**（cron + 前端 + 后端）。

也可只用 cron 自动修复（省心），或测完后再批量修（简单）。

---

## D.4 验证修复 + 最终报告

```
@test-engineer 验证所有模块的 retest issue，输出全工程回归报告
```

最终报告包括：
- 各模块测试通过率和变化趋势（对比上次回归）
- 遗留问题清单和风险评估
- 是否建议发布

---

## D.5 发布决策

```
@devops-engineer 全工程回归通过率 93.7%，9 个 bug 已全部修复，准备发布 v1.0.0
```

运维工程师基于回归报告执行发布流程。

---

## 场景 D 总结

| 步骤 | 用户只说 | 团队自主完成 |
|------|---------|-------------|
| 启动 | `@test-engineer 全工程回归测试` | 列出模块→排序→逐模块测试→汇总 |
| 修复 | **并行**：另开终端 `@frontend-dev/@backend-dev 持续监控修复`，cron 同步工作 | 3 条线同时修 bug |
| 验证 | `@test-engineer 验证 retest，输出全工程回归报告` | 逐条复查→通过率统计→风险评估→发布建议 |
| 发布 | `@devops-engineer 发布 v1.0.0` | 基于回归报告→构建→部署 |

---

---

## 并行会话指南

多个 Claude Code 会话可以同时运行，通过共享文件（task-board.md、test/issue/）进行协调，互不冲突。

### 在 VS Code 中开启并行会话

1. `Ctrl+Shift+` 打开新终端（可开多个）
2. 每个终端运行独立的 Claude Code 会话
3. 每个会话分配不同角色，共同读写同一套协作文件

### 适用场景

| 组合 | 终端 1 | 终端 2 | 终端 3 |
|------|--------|--------|--------|
| 场景 A 并行开发 | `@frontend-dev 开发` | `@backend-dev 开发` | — |
| 场景 C 边测边修 | `@test-engineer 测试` | `@frontend-dev 监控修复` | `@backend-dev 监控修复` |
| 场景 D 全工程加速 | `@test-engineer 全工程回归` | `@frontend-dev 修复` | `@backend-dev 修复` |

### 并行安全机制

- **文件归属隔离**：前端改 `.vue/.ts`，后端改 `.java/.xml`，测试改 `test/issue/`，互不冲突
- **issue 状态锁**：`status` 字段保证同一 bug 同一时刻只有一个角色在修
- **单端/联动分离**：`assigned_to=frontend-dev|backend-dev` 的单端 bug 并行修；`assigned_to=both` 的联动 bug 在主终端串行协同修
- **cron 兜底**：即使手动会话没覆盖到，2 分钟 cron job 也会自动拾取遗漏的 issue

---

## 四种场景选择指南

| 你遇到的情况 | 走哪个流程 |
|-------------|-----------|
| 「我想给知识库加个批量导出功能」 | **场景 A**：功能开发 |
| 「知识库列表能不能加个排序」 | **场景 A**：功能开发 |
| 「搜索输入 % 号页面崩了」 | **场景 B**：Bug 修复 |
| 「上传文件后列表没刷新」 | **场景 B**：Bug 修复 |
| 「知识库详情页加载很慢」 | 先 **场景 B** 排查，若是性能瓶颈转 **场景 A** |
| 「taskManager 开发完了，全面测一下」 | **场景 C**：单模块完整测试 |
| 「Excel 导入功能需要重点测一遍」 | **场景 C**：单模块完整测试 |
| 「v1.0.0 发布前，做一次全量回归」 | **场景 D**：全工程回归测试 |
| 「月底了，做一次全工程质量检查」 | **场景 D**：全工程回归测试 |

> 简单判断：
> - **「缺功能」→ 场景 A**
> - **「功能坏了」→ 场景 B**
> - **「XX 模块测一遍」→ 场景 C**
> - **「整个工程测一遍 / 发布前检查」→ 场景 D**

---

## VS Code 工作区建议

| 操作 | 方式 |
|------|------|
| 查看当前迭代状态 | 侧边栏展开 `.claude/team/`，点击 `focus.md` |
| 查看任务进度 | 侧边栏点击 `task-board.md` |
| 查看测试质量 | 侧边栏展开 `test/issue/app/`，点击对应模块 |
| 查看发布历史 | 侧边栏点击 `release-notes.md` |
| 给团队下指令 | `Ctrl+Shift+P` → `Claude Code: Open` |
| 启动前端预览 | 终端 `cd superbox-frontend && npm run dev` |

> **提示**：将 `.claude/team/` 和 `test/issue/app/` 在 VS Code 文件浏览器中加为收藏（右键 → Add to Favorites），随时掌握团队状态。
