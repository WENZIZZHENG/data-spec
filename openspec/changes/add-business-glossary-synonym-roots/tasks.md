## 1. OpenSpec 准备

- [x] 1.1 确认 change id `add-business-glossary-synonym-roots`、capability `business-glossary-synonym-roots`、验收标准和不做边界。
- [x] 1.2 将 TODO 交接草稿补强为 full 级 OpenSpec，覆盖数据模型、API、前端、AI Context、测试和验证。
- [x] 1.3 运行 `openspec validate add-business-glossary-synonym-roots --strict`。

## 2. 测试先行

- [x] 2.1 新增后端 glossary service/controller 测试，覆盖创建、更新、软删除、重复 term、项目边界和冲突检测。
- [x] 2.2 新增字段推荐/检索测试，覆盖“会员手机号”“订单费用”通过 glossary 命中 canonical 字段与 `术语表` 命中原因。
- [x] 2.3 新增 AI Context 测试，覆盖 `field-catalog.json` 导出精简 `glossary` 和超限 warning。
- [x] 2.4 新增前端 API/page 测试或构建覆盖，确保术语表页面类型与基础交互可编译。
- [x] 2.5 运行新增失败测试，确认失败原因来自功能缺失。

## 3. 后端实现

- [x] 3.1 新增 Flyway 迁移 `ds_business_glossary`，包含注释、唯一约束、项目/status/canonical 字段索引。
- [x] 3.2 新增 glossary entity/mapper/repository/model/service/controller，沿用现有 `R`、`PageResult`、`BizException`、`ProjectAccessGuard` 模式。
- [x] 3.3 实现术语规范化、CRUD 默认值、软删除、重复 active term 校验和冲突检测。
- [x] 3.4 扩展字段推荐和字段检索，使 glossary 命中参与排序、命中原因和 nextActions，但不覆盖字段已有 aliases。
- [x] 3.5 扩展 AI Context `field-catalog.json`，导出 bounded glossary、canonical 字段名和超限 warning。

## 4. 前端与文档

- [x] 4.1 新增前端 glossary API 封装和必要类型。
- [x] 4.2 新增 `BusinessGlossary.vue` 页面，支持列表、筛选、新建、编辑、禁用/启用、删除和冲突摘要。
- [x] 4.3 在路由与侧边栏“基础数据”中加入业务术语表入口。
- [x] 4.4 更新 README 当前能力与使用说明，更新 TODO P6-48 状态和后续边界。

## 5. 验证与收口

- [x] 5.1 运行 `mvn test`。
- [x] 5.2 运行 `pnpm build`。
- [x] 5.3 运行 `openspec validate add-business-glossary-synonym-roots --strict` 和 `git diff --check`。
- [x] 5.4 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [ ] 5.5 在 proposal 增加 Verification Evidence，提交实现并归档 OpenSpec change。
