# db-reverse-import-frontend Specification

## Purpose
TBD - created by archiving change enhance-db-reverse-import-frontend. Update Purpose after archive.
## Requirements
### Requirement: 数据库直连流程步骤化
反向导入页在数据库直连模式下 SHALL 以连续步骤呈现连接信息、选择表、预览确认、导入结果，且 SHALL 根据当前操作状态高亮当前步骤。

#### Scenario: 用户进入数据库直连模式
- **WHEN** 用户在反向导入页切换到数据库直连
- **THEN** 页面展示数据库连接、选择表、预览确认、导入结果四个步骤和当前项目名称

#### Scenario: 用户完成预览
- **WHEN** 用户选择表并成功生成反向导入预览
- **THEN** 页面高亮预览确认步骤，并展示字段候选、缺注释和非标准字段摘要

### Requirement: 表选择可批量操作
反向导入页 SHALL 支持用户在已加载数据库表后搜索、全选、清空表，并展示当前已选数量。

#### Scenario: 用户搜索并选择表
- **WHEN** 数据库表已加载且用户输入搜索关键字
- **THEN** 页面只展示匹配的表，并保留当前已选表数量

#### Scenario: 用户批量选择表
- **WHEN** 数据库表已加载且用户点击全选或清空
- **THEN** 页面更新待预览表集合，并同步更新已选数量

### Requirement: 候选字段可确认导入
反向导入页 SHALL 在预览结果中按表组织字段候选，并允许用户勾选本次要导入的候选字段；确认导入时 MUST 只提交已勾选候选字段。

#### Scenario: 用户勾选部分字段导入
- **WHEN** 预览结果包含多个字段候选且用户取消勾选其中一部分
- **THEN** 确认导入只提交仍被勾选的字段候选

#### Scenario: 用户未选择候选字段
- **WHEN** 预览结果存在但用户未勾选任何候选字段
- **THEN** 确认导入操作不可用或给出明确提示

### Requirement: 导入结果可收尾
反向导入页 SHALL 在导入完成后展示新增和跳过数量，并提供查看字段库的入口。

#### Scenario: 用户完成确认导入
- **WHEN** 后端返回导入结果
- **THEN** 页面展示新增字段数、跳过字段数、字段列表摘要和查看字段库入口
