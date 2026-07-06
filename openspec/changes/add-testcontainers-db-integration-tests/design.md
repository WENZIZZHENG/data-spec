## Context

DataSpec 的数据库直连链路已经包含 `JdbcDatabaseMetadataAdapter`、`DatabaseReverseImportServiceImpl`、`FieldCoverageServiceImpl` 和数据库连接安全/健康诊断。现有测试覆盖 H2、mock JDBC metadata 和 schema dump fixture，但 PostgreSQL/MySQL 驱动在 COMMENT、catalog/schema、索引、大小写和权限诊断上存在真实方言差异。

本变更按 SDD full 处理，因为它新增 Maven 测试 profile 和 Testcontainers 依赖，属于构建/验证环境能力变更。实现必须保持生产代码契约不变，并确保默认 `mvn test` 不依赖 Docker。

## Goals / Non-Goals

**Goals:**
- 提供一个显式 opt-in 的后端真实数据库集成测试入口。
- 用 PostgreSQL/MySQL Testcontainers 加载最小 schema fixture，验证 table list、metadata dump/browser、compare、coverage 和连接健康/权限诊断关键字段。
- 保持容器测试只读读取 metadata，不扫描业务数据行，不保存 raw password、JDBC URL、token 或 DSN。
- 将验证命令和边界写入 README/TODO 与 OpenSpec Verification Evidence。

**Non-Goals:**
- 不把 Docker 作为默认开发门禁，`mvn test` 继续可在无 Docker 环境运行。
- 不扩展生产 API、OpenAPI 字段、数据库 schema 或前端页面。
- 不覆盖所有数据库版本和供应商兼容性；第一版只覆盖项目已支持的 PostgreSQL/MySQL 关键路径。

## Decisions

1. **使用 Maven profile 隔离容器测试**
   - 选择新增 `db-integration` profile，并通过 Surefire include pattern 只在该 profile 下运行 `*IT.java`。
   - 替代方案是在默认 `mvn test` 中用 JUnit condition 跳过容器测试；缺点是默认测试仍会解析容器依赖和跳过逻辑，输出噪声更大。

2. **使用 Testcontainers JDBC 驱动依赖外的显式容器 API**
   - 测试类直接使用 PostgreSQLContainer/MySQLContainer，便于创建 schema、只读用户和最小 fixture。
   - 替代方案是使用 `jdbc:tc:` URL；它对只读用户、授权和多步骤 fixture 初始化的表达不如显式容器直观。

3. **集成测试复用现有 service/adapter 入口**
   - 表列表、dump/browser/compare/coverage 通过现有 service 方法执行，避免测试只验证驱动而绕过 DataSpec 逻辑。
   - 第一版可用轻量 in-memory repository/mock field service 支撑标准字段数据；不启动完整 Spring 上下文，减少 Docker 测试时长和脆弱性。

4. **fixture 只包含结构 metadata**
   - PostgreSQL fixture 使用 `COMMENT ON`、schema、索引和只读授权。
   - MySQL fixture 使用 inline/table comment、databaseName 作为 catalog、索引和只读授权。
   - 不插入业务数据行；如数据库要求建表，可只创建空表和 metadata。

## Risks / Trade-offs

- [Risk] 本地未安装 Docker 时 profile 测试会失败。→ Mitigation：README 明确这是可选命令；默认 `mvn test` 不启用该 profile。
- [Risk] 容器镜像下载较慢或受网络影响。→ Mitigation：只在显式 profile 中运行，并保持 fixture 最小。
- [Risk] MySQL/PostgreSQL 不同版本 metadata 表现略有差异。→ Mitigation：断言关键稳定字段，不断言无关驱动细节；失败信息包含方言和字段名。
- [Risk] 测试中出现示例密码或连接串被提交。→ Mitigation：只使用容器动态凭据，不在 README/TODO/OpenSpec 写 raw JDBC URL；提交前做敏感词扫描。

## Migration Plan

1. 新增 test scope Testcontainers 依赖和 `db-integration` Maven profile。
2. 新增 `*IT.java` 集成测试，先验证默认命令不运行，再通过 profile 运行。
3. 更新 README/TODO 和 OpenSpec tasks 的 Verification Evidence。
4. 回滚时删除 profile、Testcontainers 依赖、集成测试和文档记录即可，不涉及生产数据或 API 迁移。

## Open Questions

- 无待用户确认问题；第一版按 P6-82 TODO 边界实现 PostgreSQL/MySQL 最小真实数据库矩阵。
