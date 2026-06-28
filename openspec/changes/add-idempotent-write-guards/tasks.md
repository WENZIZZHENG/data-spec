## 1. OpenSpec 与范围确认

- [x] 1.1 创建 P6-40 proposal、design、delta specs 和 tasks。
- [x] 1.2 通过 OpenSpec change 校验。

## 2. 后端幂等与任务锁

- [x] 2.1 新增轻量 `WriteGuardService` 或等价服务，支持 idempotency key、项目级 operation try-lock、成功结果缓存和锁冲突异常。
- [x] 2.2 扩展 `ErrorCatalog`，将幂等/任务锁冲突映射为 retryable 的 AI/CLI 可读错误。
- [x] 2.3 标准快照创建、反向导入确认、AI 批量 SQL lint、项目恢复 apply 接入 header/service key。
- [x] 2.4 AI job 回放记录按稳定请求指纹去重，避免同一请求重试生成重复记录。

## 3. CLI 与契约

- [x] 3.1 CLI 写入命令支持 `--idempotency-key` 和 `DATASPEC_IDEMPOTENCY_KEY`，请求时透传 `Idempotency-Key` header。
- [x] 3.2 更新 README / TODO，说明 P6-40 已完成能力和单机边界。

## 4. 测试、评审与归档

- [x] 4.1 增加后端单测，覆盖重复 key 复用结果、无 key 时项目锁冲突、AI job 指纹去重和目标服务接入。
- [x] 4.2 增加 CLI 测试，覆盖参数和环境变量 header 透传。
- [x] 4.3 执行 `mvn test`、`node --test tools/dataspec-cli.test.mjs`、`npx.cmd openspec validate --all` 和 `git diff --check`。
- [x] 4.4 完成本地结构化代码评审并修复 findings。
- [ ] 4.5 创建本地 commit，归档 OpenSpec change 并再次验证。
