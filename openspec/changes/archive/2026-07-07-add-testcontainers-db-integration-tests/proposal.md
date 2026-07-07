## Why

数据库直连反向导入、metadata 浏览、二次比对和覆盖率报告已经依赖 PostgreSQL/MySQL 的真实 JDBC metadata 行为；当前主要靠 H2、mock 和 JSON fixture，无法稳定发现 COMMENT、schema/catalog、索引、大小写和只读权限在真实数据库中的漂移。P6-82 需要增加一个可选真实数据库集成测试入口，让这些方言边界可以在本地或 CI 中按需验证。

## What Changes

- 新增基于 Testcontainers 的后端集成测试 profile，用专门 Maven 命令启动 PostgreSQL/MySQL 容器并加载最小 schema fixture。
- 新增真实数据库集成测试，覆盖表列表、metadata dump/browser、反向导入 compare、覆盖率报告和连接健康/权限诊断的关键字段。
- 保持默认 `mvn test` 不依赖 Docker；没有显式 profile 时不启动容器、不下载数据库镜像。
- 在 OpenSpec 和 README/TODO 中记录验证命令、边界和失败定位方式。

## Capabilities

### New Capabilities
- `db-testcontainers-integration-tests`: 覆盖真实 PostgreSQL/MySQL 容器集成测试矩阵、可选运行 profile、验证范围、Docker 边界和敏感信息约束。

### Modified Capabilities
- `db-metadata-dump`: 增加真实数据库 metadata dump 的验证要求，不改变现有 API 响应契约。
- `db-reverse-import-compare`: 增加真实数据库二次比对的验证要求，不改变现有 compare 结果字段。
- `field-coverage-report`: 增加真实数据库覆盖率报告的验证要求，不改变现有覆盖率结果字段。
- `db-readonly-security-diagnostics`: 增加真实数据库只读/写风险诊断的验证要求，不改变现有诊断字段。

## Impact

- 代码：`dataspec-server/pom.xml`、后端测试源码和必要测试 fixture。
- 依赖：新增 test scope 的 Testcontainers PostgreSQL/MySQL/JUnit 支持；默认构建路径不启用容器。
- 验证：新增 `mvn test -Pdb-integration` 或等价 profile 命令；继续保留 `mvn test` 作为无 Docker 的基础门禁。
- 文档：README 验证章节和 TODO P6-82 状态需要同步。
