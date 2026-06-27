## 1. OpenSpec 与测试基线

- [x] 1.1 创建 P6-2 proposal/design/spec/tasks，并通过 OpenSpec validate。
- [x] 1.2 后端先新增字段覆盖率服务测试，验证 summary、状态分类和空输入错误。

## 2. 后端覆盖率报告

- [x] 2.1 新增 coverage 模块模型：summary、table summary、field item、unmanaged ranking、status enum。
- [x] 2.2 实现 `FieldCoverageService`，支持从 `TableDef` 列表生成报告。
- [x] 2.3 标准字段名/别名命中计入覆盖，缺注释单独统计，疑似重复只提示不计入覆盖。
- [x] 2.4 新增 Controller：SQL/DDL report API 和数据库直连 report API。
- [x] 2.5 复用数据库直连 metadata 读取能力，不扫描业务数据行、不保存连接密码。

## 3. 前端覆盖率页面

- [x] 3.1 新增前端 API/types 和覆盖率展示工具函数测试。
- [x] 3.2 新增“数据管理 / 覆盖率报告”页面，支持连接、加载表、选择表、生成报告。
- [x] 3.3 页面展示项目级覆盖率、表级统计、字段明细、未纳管排行，并支持表/状态筛选。
- [x] 3.4 未纳管字段提供跳转反向导入或字段库查询入口。

## 4. 文档与收尾

- [x] 4.1 更新 README/TODO，记录 P6-2 状态、使用方式和边界。
- [x] 4.2 运行后端测试、前端测试/构建、OpenSpec validate 和 diff 检查。
- [x] 4.3 进行直接代码评审并修复 findings。
