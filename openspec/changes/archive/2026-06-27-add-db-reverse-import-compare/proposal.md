## Why

数据库直连反向导入第一版已经能把现有表结构导入为字段候选，但用户再次连接同一数据库时，仍然只能重新预览候选，无法快速判断“数据库现状”和“DataSpec 字段标准”之间发生了什么变化。新增二次比对可以把反向导入从一次性初始化工具，升级为日常维护入口。

## What Changes

- 新增数据库直连 compare API，复用已有连接信息、表选择和 JDBC metadata 读取。
- 返回按表分组的差异摘要和明细，覆盖新增字段、已匹配字段、类型/长度/默认值/注释变化、缺注释和非标准字段。
- 前端反向导入页新增“生成差异”入口和差异视图，支持按状态筛选。
- 保持确认导入仍由现有候选导入流程处理，不自动修改源数据库或删除 DataSpec 字段。

## Capabilities

### New Capabilities

- `db-reverse-import-compare`: 数据库直连反向导入支持当前数据库 schema 与 DataSpec 字段标准的只读二次比对。

### Modified Capabilities

无。

## Impact

- 影响后端 `reverseimport` controller/service/model 和相关单测。
- 影响前端 `dataspec-web/src/api/reverseImport.ts`、`ReverseImport.vue`、类型导出或本地类型补充。
- 更新 README/TODO 中反向导入能力说明。
- 不新增数据库迁移，不保存数据库密码，不修改源数据库。
