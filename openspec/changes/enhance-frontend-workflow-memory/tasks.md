## 1. OpenSpec 与测试

- [x] 1.1 创建 P5-8 proposal/design/spec/tasks，并通过 OpenSpec validate。
- [x] 1.2 新增 reverse import memory 前端测试，先验证失败，再实现。

## 2. 前端状态记忆

- [x] 2.1 新增 reverseImportMemory utility，封装项目级 key、状态清洗、读写和字段库跳转 query。
- [x] 2.2 改造 ReverseImport.vue，按项目恢复和保存非敏感数据库直连状态、表选择、搜索词、差异筛选和 activeMode。
- [x] 2.3 确认密码不会被本地持久化，恢复后不自动触发数据库连接。

## 3. 字段库筛选联动

- [x] 3.1 改造 FieldLibrary.vue，支持 route query keyword 初始化和变化同步。
- [x] 3.2 反向导入结果跳转字段库时携带最近导入字段关键词。

## 4. 文档、评审与验证

- [x] 4.1 更新 TODO/README，记录 P5-8 第一版状态和边界。
- [x] 4.2 运行前端测试、前端构建、OpenSpec validate 和 diff 检查。
- [x] 4.3 进行直接代码评审并修复 findings。
