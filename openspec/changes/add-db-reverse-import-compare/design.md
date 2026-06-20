## Context

当前直连反向导入已有 `/api/reverse-import/database/preview`，后端读取 JDBC metadata 后转成 `TableDef`，再复用 `ReverseImportService.previewTables()` 生成字段候选、缺注释和非标准字段。这个模型适合首次导入，但对“再次连接后看变化”不够直观，因为候选列表只表达未命中标准字段，不能展示已匹配字段和字段属性变化。

## Goals / Non-Goals

**Goals:**

- 复用现有数据库连接、表选择、metadata 读取，不新增连接存储。
- 新增只读 compare API，按表输出 `NEW`、`MATCHED`、`CHANGED`、`MISSING_COMMENT`、`NON_STANDARD` 等差异状态。
- 使用当前项目标准字段名和 aliases 建索引，并对比字段类型、nullable、defaultValue、comment。
- 前端在反向导入页展示差异摘要、按状态筛选和表分组明细。

**Non-Goals:**

- 不保存数据库密码或连接配置。
- 不定时同步，不自动修改源数据库。
- 不自动删除或废弃 DataSpec 中不存在于数据库的字段。
- 不引入复杂 schema diff/migration planner。

## Decisions

1. **compare 复用 preview 的 metadata 读取链路**
   - 理由：已有 H2/JDBC metadata 测试覆盖，避免重复处理 PostgreSQL/MySQL 差异。
   - 替代方案：单独写 metadata scanner；会增加方言兼容维护成本。

2. **标准字段匹配以字段名和 aliases 为主**
   - 理由：当前字段标准就是项目级字段库，aliases 已经用于预览候选判断。
   - 对比属性只在命中标准字段时进行；未命中字段作为 `NEW`/`NON_STANDARD`。

3. **差异模型面向前端展示，而不是迁移执行**
   - 返回 summary、tableDiffs 和 fieldDiffs，字段级 changes 包含 `property/currentValue/standardValue`。
   - 这让前端能展示清晰原因，也为未来来源追踪或导入更新保留扩展点。

## Risks / Trade-offs

- **类型字符串来自 JDBC，可能大小写或方言不同** → 第一版做大小写无关和空白归一化，不做复杂类型等价矩阵。
- **注释变化判断可能受数据库 metadata 支持影响** → 只在 metadata 返回注释时与标准字段 comment 对比；空注释单独标记缺注释。
- **同名字段跨表语义不同** → 仍按项目字段标准全局匹配，保持与现有字段库一致；后续如需要再引入表级来源追踪。
- **前端状态过多** → 第一版用状态筛选和表分组减少噪音，不做复杂 diff 编辑器。
