## 1. OpenSpec

- [x] 1.1 创建 P6-16 OpenSpec change，并通过 `openspec validate add-performance-baseline-observability`。

## 2. 后端性能探针

- [x] 2.1 新增轻量 `PerformanceProbe`，支持测量耗时、慢操作 warning 和诊断 hint。
- [x] 2.2 给字段列表、字段分组、字段推荐、AI Context field catalog/package、SQL 检查记录分页和反向导入 compare 加慢操作探针。

## 3. 性能基线测试

- [x] 3.1 新增合成大字段库性能基线测试，构造千级字段和千级 compare 列。
- [x] 3.2 测试输出可读 metric 行，并断言核心结果正确、耗时未明显退化。
- [x] 3.3 将测试接入 `mvn test`，避免只靠手动脚本。

## 4. 文档与待办

- [x] 4.1 更新 README，说明性能基线命令、慢操作日志和边界。
- [x] 4.2 更新 TODO，将 P6-16 标记为已完成第一版并推进下一步顺序。

## 5. 验证、评审与提交

- [x] 5.1 运行目标测试、`mvn test`、OpenSpec validate 和 `git diff --check`。
- [x] 5.2 进行直接代码评审，不使用子 agent；修复 findings 或记录暂不处理理由。
- [x] 5.3 创建本地 commit 后继续下一个待办。
