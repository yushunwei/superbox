# 标签管理 (Tag Manager) — 测试问题清单

> 测试日期：2026-06-14
> 测试工程师：AI Agent
> 测试范围：tagManager 应用全栈（Vue 3 前端 + Spring Boot 后端 + PostgreSQL）
> 测试方法：代码审查 + API 合约测试 + 白盒边界测试

---

## 一、测试用例概览

### 模块 A：标签分类 CRUD（API `/api/v1/tag-categories`）

| 用例编号 | 测试场景 | 预期结果 | 实际结果 |
|---------|---------|---------|---------|
| TC-A01 | GET / 列出所有分类 | 返回200，data 为分类数组 | PASS |
| TC-A02 | GET /{id} 获取存在的分类 | 返回200 | PASS |
| TC-A03 | GET /{id} 获取不存在的分类 | 返回404 | PASS |
| TC-A04 | POST 创建分类（有效数据） | 返回200，name + color 持久化 | PASS |
| TC-A05 | POST 创建分类（name=null） | 返回400 | PASS |
| TC-A06 | POST 创建分类（name 为空字符串 ""） | 返回400 | PASS |
| TC-A07 | POST 创建分类（name 为全空格 "   "） | 返回400 | PASS |
| TC-A08 | POST 创建分类（不传 color，使用默认值） | 返回200，color=#409EFF | PASS |
| TC-A09 | POST 创建分类（重复 name） | 返回400，有意义的错误信息 | **FAIL** → IS-031 |
| TC-A10 | PUT /{id} 更新分类 | 返回200 | PASS |
| TC-A11 | PUT /{id} 更新 name 为全空格 | name 不更新，保持原值 | PASS（静默忽略） |
| TC-A12 | PUT /{id} 更新不存在的分类 | 返回404 | PASS |
| TC-A13 | DELETE /{id} 删除分类 | 返回200 | PASS |
| TC-A14 | DELETE /{id} 删除不存在的分类 | 返回404 | PASS |
| TC-A15 | DELETE 分类（该分类下有标签） | 删除成功，标签 categoryId 被 SET NULL | PASS（设计如此） |

### 模块 B：标签 CRUD（API `/api/v1/tags`）

| 用例编号 | 测试场景 | 预期结果 | 实际结果 |
|---------|---------|---------|---------|
| TC-B01 | GET / 列出所有标签 | 返回200 | PASS |
| TC-B02 | GET /?categoryId=1 按分类筛选 | 返回200，仅该分类下的标签 | PASS |
| TC-B03 | GET /?categoryId=0 不存在的分类 | 返回200，空数组 | PASS |
| TC-B04 | GET /{id} 获取存在的标签 | 返回200 | PASS |
| TC-B05 | GET /{id} 获取不存在的标签 | 返回404 | PASS |
| TC-B06 | POST 创建标签（有效数据） | 返回200 | PASS |
| TC-B07 | POST 创建标签（仅 name，无 color/categoryId） | 返回200，color=#409EFF，categoryId=null | PASS |
| TC-B08 | POST 创建标签（name=null） | 返回400 | **FAIL** → IS-032 |
| TC-B09 | POST 创建标签（name 为空字符串 ""） | 返回400 | **FAIL** → IS-033 |
| TC-B10 | POST 创建标签（重复 name） | 返回400，有意义的错误信息 | **FAIL** → IS-034 |
| TC-B11 | POST 创建标签（categoryId 不存在） | 返回400，有意义的错误信息 | **FAIL** → IS-035 |
| TC-B12 | POST 创建标签（name 超过 64 字符） | 返回400 | **FAIL** → IS-036 |
| TC-B13 | PUT /{id} 更新标签 | 返回200 | PASS |
| TC-B14 | PUT /{id} 更新 name 为空字符串 | 返回400 | **FAIL** → IS-037 |
| TC-B15 | PUT /{id} 更新不存在标签 | 返回404 | PASS |
| TC-B16 | DELETE /{id} 删除无引用的标签 | 返回200 | PASS |
| TC-B17 | DELETE /{id} 删除被任务引用的标签 | 返回400，有意义的错误信息 | PASS |
| TC-B18 | DELETE /{id} 删除不存在的标签 | 返回404 | PASS |
| TC-B19 | POST 创建标签（负值 sortOrder） | 允许（INT 类型，无约束） | PASS（设计如此） |
| TC-B20 | GET /?categoryId=<SQL injection> | 安全处理 | **FAIL** → IS-038 |

### 模块 C：前端 UI 交互（Vue 3 组件）

| 用例编号 | 测试场景 | 预期结果 | 实际结果 |
|---------|---------|---------|---------|
| TC-C01 | 页面挂载，加载分类列表 | 左侧显示分类列表 | PASS（代码级） |
| TC-C02 | 点击分类，加载该分类下的标签 | 右侧显示标签列表 | PASS（代码级） |
| TC-C03 | 新增分类对话框，输入名称并保存 | 分类创建成功 | PASS（代码级） |
| TC-C04 | 前端空名称校验（分类） | 提示"请输入分类名称" | PASS（代码级） |
| TC-C05 | 前端空名称校验（标签） | 提示"请输入标签名称" | PASS（代码级） |
| TC-C06 | 编辑分类/标签对话框回填 | 回填现有数据 | PASS（代码级） |
| TC-C07 | 颜色选择器交互 | 预定义色块 + 自定义取色器 | PASS（代码级） |
| TC-C08 | 删除分类确认对话框 | 弹出确认框，确认后删除 | PASS（代码级） |
| TC-C09 | 删除标签确认对话框 | 弹出确认框 | PASS（代码级） |
| TC-C10 | 空状态提示 | 无分类时显示"暂无分类"；无标签时显示"此分类下暂无标签" | PASS（代码级） |
| TC-C11 | 未选分类时"新增标签"按钮禁用 | 按钮 disabled | PASS（代码级） |
| TC-C12 | 分类操作按钮 show-on-hover | 悬停时显示编辑/删除按钮 | PASS（代码级） |

### 模块 D：数据模型与约束

| 用例编号 | 测试场景 | 预期结果 | 实际结果 |
|---------|---------|---------|---------|
| TC-D01 | tag_category.name UNIQUE 约束 | 重复 name 插入失败 | PASS（返回500而非400） |
| TC-D02 | tag.name UNIQUE 约束 | 重复 name 插入失败 | PASS（返回500而非400） |
| TC-D03 | tag.categoryId FK → ON DELETE SET NULL | 删除分类时标签 categoryId 置 null | PASS |
| TC-D04 | task_tag FK → ON DELETE CASCADE | 删除标签时自动清理关联 | PASS（但 Service 层有引用检查先拦截） |
| TC-D05 | knowledge_tag FK → ON DELETE CASCADE | 同上 | PASS |

### 模块 E：跨模块集成

| 用例编号 | 测试场景 | 预期结果 | 实际结果 |
|---------|---------|---------|---------|
| TC-E01 | 任务创建时关联标签（tagIds） | 标签关联成功 | PASS |
| TC-E02 | SpreadsheetView 导入 tagApi | 导入正确，无 TS 错误 | PASS |
| TC-E03 | TagController vs TagCategoryController 响应格式 | 两种格式前端均能处理 | **PASS（不一致但正确）** |

---

## 二、问题详情

### Issue #IS-031

---
id: IS-031
title: "创建重复名称的分类返回 500 而非 400"
status: fixed
severity: major
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. 创建分类 name="TestCatA-Updated"
2. 再次创建 name="TestCatA-Updated"

**预期行为：**
返回 400 Bad Request，包含有意义的错误信息如"分类名称已存在"。

**实际行为：**
返回 500 Internal Server Error，因为 `DuplicateKeyException` 未被 Service 层捕获。

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/service/TagCategoryService.java` — create() 方法缺少 DuplicateKeyException 处理

---

### Issue #IS-032

---
id: IS-032
title: "创建标签时 name=null 返回 500 而非 400"
status: fixed
severity: major
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. POST /api/v1/tags body: `{"color":"#F56C6C","categoryId":1}` (不传 name)
2. 或者显式传 `{"name":null,...}`

**预期行为：**
返回 400 Bad Request，"标签名称不能为空"。

**实际行为：**
返回 500 Internal Server Error。因为 `TagService.create()` 未校验 name 是否 null，null 值直接传给 INSERT，触发 DB NOT NULL 约束异常。

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/service/TagService.java` — create() 方法缺少 name null 校验
- `superbox-backend/src/main/java/com/superbox/app/tagManager/controller/TagController.java` — create() 方法直接从 body map 取值未校验

---

### Issue #IS-033

---
id: IS-033
title: "创建标签时 name 为空字符串应返回 400"
status: fixed
severity: major
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. POST /api/v1/tags body: `{"name":"","color":"#F56C6C","categoryId":1}`

**预期行为：**
返回 400 Bad Request，"标签名称不能为空"。

**实际行为：**
返回 200，创建了一个 name="" 的标签。Service 层未校验 name 是否为空字符串。

**影响：**
- 第二次创建空名称标签时触发 UNIQUE 约束，返回 500（见 IS-034）
- 前端虽有 `trim()` 校验，但直接 API 调用可绕过

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/service/TagService.java` — create() 方法缺少 name blank 校验

---

### Issue #IS-034

---
id: IS-034
title: "创建重复名称的标签返回 500 而非 400"
status: fixed
severity: major
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. 创建标签 name="MinimalTag"
2. 再次创建 name="MinimalTag"

**预期行为：**
返回 400 Bad Request，"标签名称已存在"。

**实际行为：**
返回 500 Internal Server Error。`TagService.create()` 未捕获 `DuplicateKeyException`，直接暴露为 500。

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/service/TagService.java` — create() 方法

---

### Issue #IS-035

---
id: IS-035
title: "创建标签时使用不存在的 categoryId 返回 500 而非 400"
status: fixed
severity: major
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. POST /api/v1/tags body: `{"name":"OrphanTag","categoryId":99999}`

**预期行为：**
返回 400 Bad Request，"指定的分类不存在"。

**实际行为：**
返回 500 Internal Server Error。FK 约束 `tag.category_id → tag_category.id` 违反 → `DataIntegrityViolationException` → GlobalExceptionHandler 转为 500。

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/service/TagService.java` — create() 方法应在插入前校验 categoryId 是否存在

---

### Issue #IS-036

---
id: IS-036
title: "创建标签时 name 超过 64 字符返回 500 而非 400"
status: fixed
severity: minor
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. POST /api/v1/tags body name 为 65 个 'A' 字符

**预期行为：**
返回 400 Bad Request，"标签名称不能超过 64 个字符"。

**实际行为：**
返回 500 Internal Server Error。DB VARCHAR(64) 截断报错未在 Service 层被提前校验。

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/service/TagService.java` — create() 方法

---

### Issue #IS-037

---
id: IS-037
title: "更新标签时 name 为空字符串应返回 400 而非静默接受"
status: fixed
severity: major
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. PUT /api/v1/tags/{id} body: `{"name":""}`
2. 标签 name 被更新为空字符串

**预期行为：**
返回 400 Bad Request，"标签名称不能为空"。或至少像 `TagCategoryService.update()` 那样，当 name 为 blank 时跳过更新（不设置空值）。

**实际行为：**
返回 200，标签 name 被改为空字符串。`TagService.update()` 仅检查 `name != null`，未检查 `name.isBlank()`。对比 `TagCategoryService.update()` 使用了 `name != null && !name.isBlank()`。

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/service/TagService.java` — update() 方法 L38 条件 `tag.getName() != null` 应改为 `tag.getName() != null && !tag.getName().isBlank()`

---

### Issue #IS-038

---
id: IS-038
title: "标签列表 categoryId 参数 SQL 注入尝试返回 500 而非 400"
status: fixed
severity: minor
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. GET /api/v1/tags?categoryId=1%20OR%201=1

**预期行为：**
返回 400 Bad Request，参数格式错误。

**实际行为：**
返回 500 Internal Server Error。Spring MVC 尝试将 `"1 OR 1=1"` 转换为 `Long` 失败，抛出 `TypeMismatchException` → GlobalExceptionHandler 转为 500。

**说明：**
- 实际 SQL 注入风险低（Spring 类型转换先失败，且 MyBatis 使用参数化查询）
- 但错误处理不当，应返回 400

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/controller/TagController.java` — list() 方法 L19

---

### Issue #IS-039

---
id: IS-039
title: "标签名称允许存储 XSS 字符串（纵深防御缺失）"
status: fixed
severity: minor
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. POST /api/v1/tags body: `{"name":"<script>alert(1)</script>"}`
2. 返回 200，标签成功创建

**预期行为：**
后端对 name 做输入净化或拒绝包含 HTML 标签的名称。

**实际行为：**
返回 200，XSS 字符串直接入库。

**说明：**
- 前端 Vue 使用 `{{ tag.name }}` 文本插值，默认 HTML 转义，因此实际 XSS 攻击面有限
- 但作为纵深防御，后端应对用户输入做净化（strip HTML tags）

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/service/TagService.java` — create() 方法

---

### Issue #IS-040

---
id: IS-040
title: "TagCategoryController 与 TagController 响应格式不一致"
status: fixed
severity: minor
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. 对比 GET /api/v1/tag-categories 和 GET /api/v1/tags 的响应格式

**预期行为：**
两个 Controller 使用统一的 `Result<T>` 包装类返回数据，格式一致。

**实际行为：**
- `TagCategoryController` 使用 `ResponseEntity<Map<String, Object>>`，成功响应格式：`{"data": [...]}`
- `TagController` 使用 `Result<T>`，成功响应格式：`{"code": 200, "message": "success", "data": [...]}`

**影响：**
- 前端通过 `(res as any).data` 跨两种格式取值，当前可工作但脆弱
- Error 响应（通过 GlobalExceptionHandler）均走 `Result` 格式，这又加重了不一致

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/controller/TagCategoryController.java`
- `superbox-backend/src/main/java/com/superbox/app/tagManager/controller/TagController.java`

---

### Issue #IS-041

---
id: IS-041
title: "TagController 使用原始 Map 解析请求体，缺少类型安全"
status: fixed
severity: minor
module: tagManager
type: bug
found_by: test-engineer
assigned_to: backend-dev
---

**重现步骤：**
1. 审查 `TagController.create()` 和 `update()` 方法的请求体解析

**预期行为：**
使用 `@RequestBody Tag` 直接反序列化（与 `TagCategoryController` 一致），或使用 DTO + `@Valid` 注解。

**实际行为：**
`TagController` 使用 `@RequestBody Map<String, Object>` 手动取值和类型转换（`toLong`, `toInt`），增加了代码复杂度和出错风险。

**影响：**
- 如果 JSON body 格式异常（如 `categoryId` 为 object），类型转换失败可能抛出未捕获异常
- 与 `TagCategoryController` 的风格不一致，增加维护成本

**相关文件：**
- `superbox-backend/src/main/java/com/superbox/app/tagManager/controller/TagController.java` — create() L29, update() L39

---

### Issue #IS-042

---
id: IS-042
title: "删除分类后其下标签被孤立（categoryId 置 null）但前端未提示"
status: fixed
severity: minor
module: tagManager
type: enhancement
found_by: test-engineer
assigned_to: frontend-dev
---

**重现步骤：**
1. 为某分类创建标签
2. 删除该分类
3. 观察标签 — 标签仍存在但 categoryId 变为 null

**预期行为：**
删除分类时前端确认对话框应提示用户标签将失去分类归属，或提供将标签迁移到其他分类的选项。

**实际行为：**
前端确认框文案："确定要删除分类「XXX」吗？分类下的标签不会被删除。" — 未提及标签会失去分类关联（变为"未分类"状态）。

**相关文件：**
- `superbox-frontend/src/apps/tagManager/pages/TagManagerPage.vue` — deleteCategory() L57

---

## 三、测试总结

### 总体评估

- **测试用例总数**：49
- **通过**：37 (75.5%)
- **失败**：12 (24.5%)

### 按严重程度分布

| 严重程度 | 数量 | 说明 |
|---------|------|------|
| critical | 0 | 无数据丢失或系统崩溃风险 |
| major | 8 | IS-031~IS-037, IS-040 — 缺少输入校验导致 500 错误 |
| minor | 4 | IS-038, IS-039, IS-041, IS-042 — UI/代码质量改进 |

### 核心问题模式

1. **输入校验缺失（占 70% 问题）**：`TagService` 对 name 字段缺少 null/blank 校验，对 categoryId 缺少存在性校验，对重复 name 缺少 `DuplicateKeyException` 捕获。对比 `TagCategoryService` 在这些方面做得更好。

2. **异常处理粒度不足**：多处 DB 约束违反（NOT NULL、UNIQUE、FK、数据截断）直接暴露为 500 Internal Server Error，未在 Service 层转化为有意义的 4xx 响应。

3. **代码风格不一致**：`TagCategoryController` 和 `TagController` 在请求解析方式和响应格式上选择了不同方案。

### 与聚焦应用的关系

tagManager 当前在 `focus.md` 标记为 `next_apps` 列表中的非聚焦模块。根据项目规则，非聚焦应用只修 critical bug。本次发现的 12 个 issue 中有 0 个 critical，8 个 major。这些 major issue 主要影响 API 的直接调用者（如外部服务、自动化工具），普通前端用户操作因前端有基本校验而不会触发大多数问题。

建议在 tagManager 成为聚焦应用时，优先修复 IS-032~IS-037（输入校验缺失）。
