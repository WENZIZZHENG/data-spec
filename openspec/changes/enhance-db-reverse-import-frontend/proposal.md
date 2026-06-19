## Why

数据库直连反向导入已经打通第一版，但前端仍像一组工程表单，用户需要自己理解“连接、选表、预览、导入”的先后关系。现在项目优先服务个人/小团队自用，应把这条路径打磨成从已有数据库生成数标的主流程。

本变更采用 standard SDD：它改变用户可见的前端流程和验收方式，但不修改数据库存储、权限、安全模型或后端写入契约。

## What Changes

- 将反向导入页的数据库直连模式调整为连续步骤：连接信息、选择表、预览与勾选、确认导入、查看结果。
- 补齐连接状态反馈、表搜索、全选/清空、已选数量和加载状态。
- 将预览结果按表组织，支持候选字段勾选，并在导入前展示新增/跳过等确认信息。
- 导入完成后提供结果摘要和跳转字段库的收尾动作。
- 不保存数据库密码，不自动同步，不修改源数据库，不引入审批流。

## Capabilities

### New Capabilities

- `db-reverse-import-frontend`: 数据库直连反向导入前端主流程，包括步骤化操作、表选择、候选字段确认和导入结果反馈。

### Modified Capabilities

无。

## Impact

- 主要影响 `dataspec-web/src/views/ReverseImport.vue`。
- 复用已有 `dataspec-web/src/api/reverseImport.ts` 和 `dataspec-web/src/types/index.ts` 类型。
- 验证以 `dataspec-web` 的 `pnpm build` 为类型和构建门禁；必要时补充前端单元测试或轻量测试入口。
