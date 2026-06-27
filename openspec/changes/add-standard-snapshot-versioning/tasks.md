## 1. OpenSpec 与测试基线

- [x] 1.1 创建 P6-1 proposal/design/spec/tasks，并通过 OpenSpec validate。
- [x] 1.2 后端新增标准快照服务测试，先验证失败，再实现。

## 2. 标准快照后端

- [x] 2.1 新增 V9 迁移：`ds_standard_snapshot` 表和 `ds_sql_check_record` 快照引用列。
- [x] 2.2 新增快照 entity/mapper/repository/service，支持创建、最新快照和列表。
- [x] 2.3 实现确定性 payload JSON 与 SHA-256 hash。
- [x] 2.4 新增快照 Controller，提供创建、最新快照和列表 API。

## 3. 核心链路版本引用

- [x] 3.1 AI Context manifest、field-catalog、rules.yaml 携带 `specVersion`、`specHash`、`snapshotId`。
- [x] 3.2 SQL 检查记录保存当前快照 ID/version/hash，无快照时保持兼容。
- [x] 3.3 DDL 生成结果返回标准快照元数据。

## 4. 前端与文档

- [x] 4.1 视范围新增最小前端入口或 API 类型，至少保证用户能通过 API 创建快照。
- [x] 4.2 更新 README/TODO，记录 P6-1 状态、使用方式和无快照边界。

## 5. 评审与验证

- [x] 5.1 运行后端测试、前端测试/构建、OpenSpec validate 和 diff 检查。
- [x] 5.2 进行直接代码评审并修复 findings。
