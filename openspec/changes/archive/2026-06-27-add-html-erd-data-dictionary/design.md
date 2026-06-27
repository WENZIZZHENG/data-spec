## Context

当前 `MarkdownGeneratorService` 已能生成项目级 Markdown 数据字典，`GeneratorController` 暴露 Markdown 预览/下载和 DDL 预览接口，前端 `Generator.vue` 主要围绕 DDL 生成。P4-7 需要让数据字典更适合浏览和分享，但项目仍定位个人/小团队工具，因此不需要在线文档站、权限模型或复杂图数据库。

## Goals / Non-Goals

**Goals:**

- 生成可离线打开的 HTML 数据字典，包含概览、数据域、标准字段、枚举字典和表模板。
- 生成 Mermaid ERD/关系图文本，展示数据域、字段、枚举字典、模板、模板字段之间的轻量关系。
- 后端提供 HTML/ERD 预览与下载接口。
- 前端生成器页提供 HTML 预览、ERD 预览和下载入口。
- 保持现有 Markdown API 和 DDL 生成能力兼容。

**Non-Goals:**

- 不做在线文档站、发布流程、协同编辑或访问权限。
- 不做真实数据库表关系推断，不伪造外键。
- 不引入服务端模板引擎或图形渲染依赖；第一版输出 HTML 和 Mermaid 文本。
- 不改变现有 Markdown 数据字典格式。

## Decisions

1. **新增 HTML 数据字典 service，而不是把 Markdown service 改成多格式**
   - 理由：HTML 需要转义、布局、关系图和完整页面结构，职责比 Markdown 更复杂，单独 service 更容易测试。
   - 替代方案：在 `MarkdownGeneratorService` 里加 format 参数；会让一个类同时维护两套格式细节。

2. **Mermaid 输出使用 flowchart，而不是 erDiagram**
   - 理由：DataSpec 当前管理的是标准字段、代码集、模板字段，不是真实表外键。flowchart 能准确表达“字段属于数据域”“字段引用代码集”“模板包含字段”等关系。
   - 替代方案：强行生成 erDiagram；会暗示真实数据库关系，容易误导。

3. **HTML 页面自包含 CSS 和 Mermaid 文本**
   - 理由：下载后离线可打开，且不依赖 CDN。第一版不在导出的 HTML 中加载外部 Mermaid 渲染脚本，避免离线失败。
   - 替代方案：引入 Mermaid JS 直接渲染；视觉更好但增加依赖与离线不确定性。

4. **前端复用 Generator 页面**
   - 理由：当前路由已把“数据字典生成与下载”放在 generator 模块，新增区域比新建页面更小改动。
   - 替代方案：新增独立数据字典页面；导航和页面职责会更清晰，但本轮不需要扩展到文档站。

## Risks / Trade-offs

- **HTML 转义遗漏导致页面结构错乱** → 后端统一使用 HTML escape helper，单测覆盖特殊字符。
- **关系图节点过多导致页面拥挤** → 第一版输出 Mermaid 文本和紧凑关系列表，后续再做筛选或分组。
- **字段 codeSetId 当前 Markdown 只显示 ID** → HTML service 聚合枚举字典 map，展示 code/name，避免新页面延续不直观问题。
- **前端预览 HTML 存在 XSS 风险** → 使用 `iframe srcdoc` 隔离 HTML 预览，不直接 `v-html` 注入当前应用 DOM。
