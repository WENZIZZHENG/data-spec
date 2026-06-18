## Design

### Approach

新增一个独立的 `BuiltInStandardsImportService`，负责加载 classpath 下的内置 YAML 并写入 `ds_domain`、`ds_field`。`ProjectServiceImpl.create` 在项目创建成功后调用该服务，使用事务保证项目与初始化数据一致提交。

### Key Decisions

- **默认导入**：个人/小团队自用的首要目标是新项目开箱可用，因此创建请求未传 `importBuiltInStandards` 时按 `true` 处理。
- **可跳过导入**：保留 `importBuiltInStandards=false` 给空白项目或测试场景使用。
- **classpath 资源**：运行时读取 `dataspec-server/src/main/resources/standards/**`，仓库根目录 `standards/` 继续作为项目文档和源数据参考，避免打包后读不到文件系统路径。
- **幂等写入**：导入前检查项目内 `domain.code` 和 `field.name`，已存在则跳过，便于未来暴露“初始化标准”按钮时复用同一服务。

### Risks

- 内置字段 YAML 与后端资源副本可能漂移；本轮保持内容一致，后续可考虑以构建脚本同步。
- 字段与数据域当前 YAML 未声明显式关联，因此第一版只导入数据域和字段本身，不强行推断 `domainId`。

### Verification

- 单元测试覆盖 YAML 导入、去重和项目创建后的自动导入/跳过导入。
- 后端 `mvn test` 作为统一验证入口。
- 前端 `pnpm build` 验证新增请求字段与表单类型。
- `openspec validate initialize-project-standards --strict` 验证规范。
