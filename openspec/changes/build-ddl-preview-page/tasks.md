## 1. OpenSpec

- [x] 1.1 新增 P2-2 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate build-ddl-preview-page --strict`。

## 2. 前端实现

- [x] 2.1 新增 template API wrapper。
- [x] 2.2 `types/index.ts` 导出 `Template`、`TemplateField`。
- [x] 2.3 改造 `Generator.vue`，支持当前项目模板加载、模板字段预览、表名输入和 DDL 生成。
- [x] 2.4 增加复制 DDL 与下载 `.sql` 能力。
- [x] 2.5 展示 lint 自检摘要与问题列表。

## 3. 文档与验证

- [x] 3.1 更新 TODO.md P2-2 状态。
- [x] 3.2 运行前端构建、后端测试、OpenSpec validate 和 diff 空白检查。
- [x] 3.3 直接代码评审，不使用子 agent。
- [x] 3.4 修复评审发现后创建本地 commit。
