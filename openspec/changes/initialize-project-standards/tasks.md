## 1. OpenSpec

- [x] 1.1 新增 P2-1 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate initialize-project-standards --strict`。

## 2. 后端实现

- [x] 2.1 增加 classpath 内置 standards YAML。
- [x] 2.2 增加 `BuiltInStandardsImportService`，支持导入数据域和标准字段。
- [x] 2.3 项目创建后按 `importBuiltInStandards` 默认导入内置 standards。
- [x] 2.4 补单元测试覆盖导入、去重、默认导入和跳过导入。

## 3. 前端实现

- [x] 3.1 更新 OpenAPI 类型中的 `CreateProjectReq`。
- [x] 3.2 新建项目表单增加导入内置标准开关，编辑项目时不展示。

## 4. 文档与验证

- [x] 4.1 更新 TODO.md P2-1 状态。
- [x] 4.2 运行后端测试、前端构建、OpenSpec validate 和 diff 空白检查。
- [x] 4.3 直接代码评审，不使用子 agent。
- [x] 4.4 修复评审发现后创建本地 commit。
