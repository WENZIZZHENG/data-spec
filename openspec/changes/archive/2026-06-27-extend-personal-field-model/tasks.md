## 1. OpenSpec 与测试

- [x] 1.1 验证 `extend-personal-field-model` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 扩展 AI Context 测试，先观察新增字段目录断言失败。
- [x] 1.3 新增字段服务默认值测试，先观察缺失默认元数据处理失败。

## 2. 后端实现

- [x] 2.1 新增 Flyway V3 迁移，为 `ds_field` 增加 aliases/category/code_set_id/sensitive/status/example_value。
- [x] 2.2 扩展 `Field` 实体和 `FieldController.FieldReq`。
- [x] 2.3 扩展 `FieldServiceImpl` create/update，补齐 sensitive/status 默认值并更新新增字段。
- [x] 2.4 扩展 AI field-catalog JSON 和 schema，输出 aliases 数组、sensitive、status、codeSetId、example。

## 3. 契约、文档与验证

- [x] 3.1 更新 README/TODO 与 OpenAPI TypeScript schema。
- [x] 3.2 运行后端测试、前端构建、OpenSpec validate 和 diff 空白检查。
- [x] 3.3 进行直接代码评审，检查迁移兼容性、默认值、AI 导出字段和弱关联边界。
- [x] 3.4 通过验证后提交本功能改动。
