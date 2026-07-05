## Context

P6-63 需要把现有数据库直连反向导入从“选表后直接预览/导入”扩展为“先浏览真实 schema metadata，再决定候选导入”。现有后端已有 PostgreSQL/MySQL 直连、表列表、schema dump、preview、compare、coverage；前端反向导入页已有连接信息、表选择、预览、差异和候选勾选。第一版应复用这些能力，避免新增写库流程或通用 SQL 客户端。

## Goals / Non-Goals

**Goals:**
- 新增前端元数据浏览入口或反向导入页子视图；支持按 schema/table/column/comment 搜索，展示字段标准匹配、缺注释、类型差异和可加入导入候选的勾选状态。
- 连接数据库后无需导入即可浏览元数据；AI 可读取选中表的结构摘要并继续生成候选导入或覆盖率报告；全流程只读且不采样业务数据行。

**Non-Goals:**
- 不做通用 SQL 客户端，不执行任意查询，不保存数据库密码。
- 不新增源数据库写入、业务数据采样、任意 SQL 输入框、跨库同步或自动采纳标准字段。

## Decisions

1. **沿用反向导入模块并新增只读聚合接口。**
   - 新增 `POST /api/reverse-import/database/browser`，入参复用 `DatabaseConnectionReq`。
   - 服务端打开一次连接导出 schema dump，然后基于 dump 复用 preview、compare、coverage 分析，返回浏览专用 response。
   - 原因：避免前端为了浏览连续调用多个接口，也让 AI 能拿到稳定的结构摘要。

2. **schema dump 兼容性新增 index metadata。**
   - 给 `DatabaseSchemaTable` 增加 `indexes`，每条 index 记录 indexName、columnName、nonUnique、ordinalPosition。
   - 原因：TODO 明确提到 index 浏览；该字段只增加 metadata，不改变现有 preview/compare/coverage 转换逻辑。

3. **前端在反向导入页内增加 metadata browser 子视图。**
   - 放在数据库直连 tab 的表选择区域下方，用户选表后可点击“浏览元数据”。
   - 浏览结果展示摘要、AI 摘要、字段级表格、index 标签和候选勾选；勾选状态复用既有候选导入逻辑。
   - 采用现有 Element Plus 密集表格样式，不新增独立路由，降低导航成本。

4. **安全边界保持只读。**
   - 浏览接口只读取 JDBC metadata，并复用既有只读分析服务；确认导入仍走已有显式写入接口和幂等保护。
   - 返回内容不得包含 password、token、完整 JDBC URL 或业务数据行。

## Risks / Trade-offs

- [Risk] 浏览接口聚合 preview/compare/coverage 可能增加响应耗时。→ Mitigation：第一版仅针对用户已选表执行，继续由用户控制表范围。
- [Risk] index metadata 在不同 JDBC 方言下字段质量不一致。→ Mitigation：字段可空，按 JDBC metadata best-effort 返回，并保留 warnings。
- [Risk] 前端页面已有信息密度较高。→ Mitigation：以可折叠/独立子区展示，保留现有预览和导入路径。

## Open Questions

- 无需用户额外确认；本轮按用户“自动推进所有待办”的授权实施。
