## 1. 后端 HTML/ERD 生成

- [x] 1.1 新增 HTML/ERD 数据字典生成 service，聚合项目数据域、字段、枚举字典、枚举值、模板和模板字段。
- [x] 1.2 生成自包含 HTML 文档，包含概览、字段、枚举、模板、关系图和必要 HTML 转义。
- [x] 1.3 生成 Mermaid flowchart 关系图文本，覆盖 field-domain、field-enum、template-field、template-standard-field 关系。
- [x] 1.4 扩展 `GeneratorController`，新增 HTML/ERD 预览与下载接口。
- [x] 1.5 新增后端单测覆盖 HTML 内容、HTML 转义、Mermaid 关系和空数据兼容。

## 2. 前端预览与下载

- [x] 2.1 扩展 `src/api/generator.ts`，增加 HTML/ERD 预览和下载 API。
- [x] 2.2 改造 `Generator.vue`，新增数据字典区域、HTML iframe 预览、ERD 文本预览和下载按钮。
- [x] 2.3 保持原 DDL 生成流程不回归，移动端布局不出现明显溢出。

## 3. 文档、验证与提交

- [x] 3.1 更新 README/TODO 中 P4-7 状态和功能说明。
- [x] 3.2 运行后端测试、前端测试/构建、OpenSpec validate 和 diff 检查。
- [x] 3.3 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
