## Why

DataSpec 已经能从 SQL/数据库直连反向导入字段候选，但用户还缺少一个总览来回答“当前真实库字段有多少已经被标准覆盖，哪些字段仍未纳管”。P6-2 要把反向导入和字段推荐沉淀成覆盖率报告，让个人/小团队能用真实库反馈下一轮数标维护重点。

## What Changes

- 新增字段覆盖率报告 API，基于数据库直连请求或已解析的 SQL/DDL 表结构生成覆盖率结果。
- 报告按项目输出总覆盖率、表级覆盖率和字段级状态，区分标准命中、别名命中、未命中、缺注释和疑似重复/语义相近。
- 复用现有反向导入 metadata、字段库和字段推荐逻辑，不扫描业务数据行，不写入源数据库。
- 前端新增覆盖率报告入口，支持连接数据库、选择表、生成报告、按表/状态筛选，并从未纳管字段跳转到反向导入或字段库。
- README/TODO 补充当前能力、边界和后续增强。

## Capabilities

### New Capabilities

- `field-coverage-report`: 项目级字段标准覆盖率报告，覆盖数据库直连/SQL 输入、总览统计、表级统计、字段级状态和前端查看流程。

### Modified Capabilities

无。

## Impact

- 后端：新增 coverage 模块或 reverseimport 子能力，包含 report model/service/controller；复用 `DatabaseReverseImportService`、`ReverseImportService`、`FieldService` 和字段推荐匹配逻辑。
- 前端：新增 API 封装、类型、覆盖率页面/入口和必要的展示工具函数。
- 数据：第一版不新增持久化表，报告即时生成；后续如需保存历史趋势另起任务。
- 验证：后端覆盖率服务单测、前端工具单测、`mvn test`、`pnpm test`、`pnpm build`、OpenSpec validate 和 diff 检查。
