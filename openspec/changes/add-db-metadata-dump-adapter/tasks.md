## 1. OpenSpec 与边界

- [x] 1.1 运行 `npx.cmd openspec validate add-db-metadata-dump-adapter`，确认 proposal/design/spec/tasks 可用。
- [x] 1.2 确认第一版边界：不新增持久化表、不扫描业务数据行、不保存密码/token/JDBC URL、不支持新数据库方言。

## 2. 后端 dump 模型与 adapter

- [x] 2.1 新增 `DatabaseSchemaDump`、table、column、source metadata、request model，字段命名稳定且不包含敏感字段。
- [x] 2.2 抽出 metadata adapter/converter：直连 JDBC metadata 生成 dump，dump 可转换为 `List<TableDef>`。
- [x] 2.3 支持 PostgreSQL/MySQL 方言差异：catalog/schema 映射、tableType/comment/warnings、dataType/default/nullable 保留。
- [x] 2.4 增加 dump 校验与脱敏：空项目、空表、缺列名、危险 source 字段和大文本错误有可读 BizException。

## 3. API 与服务复用

- [x] 3.1 扩展 `DatabaseReverseImportService`：新增 `exportDump`、`previewDump`、`compareDump`，现有直连 preview/compare 内部复用 dump 路径。
- [x] 3.2 扩展 coverage：新增 dump coverage 服务/接口，现有 database coverage 内部复用 dump 路径。
- [x] 3.3 扩展 Controller：新增 `/api/reverse-import/database/dump`、`/api/reverse-import/dump/preview`、`/api/reverse-import/dump/compare`、`/api/coverage/dump`。

## 4. 测试与 fixtures

- [x] 4.1 新增 schema dump fixture，覆盖 PostgreSQL/MySQL 结构 metadata、comments、nullable/default 和 warnings。
- [x] 4.2 新增 adapter/converter 单测，覆盖 dump 输出、TableDef 转换、非法 dump 和敏感信息不出现。
- [x] 4.3 新增服务/Controller 单测，覆盖 dump preview、dump compare、dump coverage 与直连路径兼容。

## 5. 文档、验证与提交

- [x] 5.1 更新 README/TODO，说明 P6-30 状态、dump API、离线边界和验证命令。
- [x] 5.2 运行 `mvn test`、`npx.cmd openspec validate add-db-metadata-dump-adapter` 和 `git diff --check`。
- [x] 5.3 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [x] 5.4 创建本地 commit，提交 P6-30 实现。
