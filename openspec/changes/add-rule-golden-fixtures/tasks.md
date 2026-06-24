## 1. Fixture 资源

- [x] 1.1 新增 PostgreSQL good SQL fixture，覆盖 COMMENT ON TABLE/COLUMN 和规范字段。
- [x] 1.2 新增 MySQL bad SQL fixture，覆盖表名/字段名、UNSIGNED、索引和缺注释样例。
- [x] 1.3 新增 fixedSql golden SQL 文件，锁定确定性修复输出。
- [x] 1.4 新增反向导入 metadata JSON fixture，覆盖候选字段、缺注释和非标准字段。

## 2. 后端测试

- [x] 2.1 新增聚合 fixture/golden 测试类，读取测试资源并复用现有 parser/lint/fixedSql/reverse import 服务。
- [x] 2.2 断言 PostgreSQL good fixture 无命名/注释误报。
- [x] 2.3 断言 MySQL bad fixture 输出关键 lint issue。
- [x] 2.4 断言 fixedSql 与 golden 文件一致。
- [x] 2.5 断言反向导入 metadata fixture 的 summary 和候选明细稳定。

## 3. 文档、验证与提交

- [x] 3.1 更新 README/TODO，说明 P5-7 第一版 fixture/golden 覆盖范围。
- [x] 3.2 运行目标后端测试，确认新增测试接入 `mvn test`。
- [x] 3.3 运行后端全量测试、OpenSpec validate 和 diff 检查。
- [x] 3.4 进行直接代码评审（不使用子 agent），修复发现的问题。
- [x] 3.5 创建本地 commit，并准备进入下一个待办。
