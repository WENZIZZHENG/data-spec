# DataSpec 待办路线图

本文件只保留当前行动入口、优先级视图和文档索引。完整候选详情已拆到 [P6 候选池](docs/todo-p6-candidates.md)，已完成 P5/P6 详情已归档到 [P5/P6 完成归档](docs/archive/todo-completed-p5-p6.md)，删除 / 不做候选记录见 [删除候选归档](docs/archive/todo-removed-p6-candidates.md)。

## 当前状态

- 近期优先行动项已清空；后续开发由用户从评审后的候选池或新需求中选择，不再从 P6-71 到 P6-188 全量顺扫。
- OpenSpec 当前无 active change；新任务开工前按 AGENTS/SDD 判断快速、常规或 SDD 流程。
- 已完成 P0-P4 详情见 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。
- 已完成 P5/P6 详情见 [docs/archive/todo-completed-p5-p6.md](docs/archive/todo-completed-p5-p6.md)，当前归档 112 项。
- 已删除 / 不做独立候选 9 项，详见 [docs/archive/todo-removed-p6-candidates.md](docs/archive/todo-removed-p6-candidates.md)。
- 未完成 P6 候选共 76 项，详情见 [docs/todo-p6-candidates.md](docs/todo-p6-candidates.md)。
- 候选池价值评审见 [docs/todo-p6-candidate-review.md](docs/todo-p6-candidate-review.md)。
- 剩余待办时间评估见 [docs/todo-p6-remaining-estimates.md](docs/todo-p6-remaining-estimates.md)。
- 任务卡可从 create-table/review-pr-sql/reverse-import-standards/export-min-context/standard-evidence-review/standard-maintenance workflow recipe 生成；该短摘要用于本地状态检查，详细能力见完成归档。
- 真实自测库授权边界：用户已授权 localhost:5432/ai_test 作为可写的一次性 PostgreSQL 测试库；仅限测试库，不扩展到其他库，不把密码写入仓库，操作后记录验证范围与清理结果。

## 下一步顺序

1. 若继续开发，先从 [候选评审](docs/todo-p6-candidate-review.md) 的“高价值保留”中选择 1 个任务，不自动线性顺扫完整候选池。
2. 开工前读取对应候选详情和 [时间评估](docs/todo-p6-remaining-estimates.md)，再按任务类型决定快速、常规、SDD standard 或 SDD full。
3. 新增想法优先合并到既有候选主题；确实无法承接时再新增编号，避免继续堆叠愿望清单。
4. 完成任务后同步根入口、候选池、OpenSpec archive 或完成归档，避免已完成能力继续压在主待办里。

## 待办入口

- [P6 候选池](docs/todo-p6-candidates.md)：保留所有未完成候选的完整背景、缺口、产物、验收标准和边界。
- [P6 候选价值评审](docs/todo-p6-candidate-review.md)：记录哪些值得做、哪些应合并、哪些已删除或暂缓。
- [P6 剩余待办时间评估](docs/todo-p6-remaining-estimates.md)：列出剩余 76 项候选和粗略开发时间。
- [P5/P6 完成归档](docs/archive/todo-completed-p5-p6.md)：保留已完成条目的验证证据、产物和后续增强。
- [P6 删除候选归档](docs/archive/todo-removed-p6-candidates.md)：记录删除独立候选的原因和恢复触发条件。
- [P0-P4 完成归档](docs/archive/todo-completed-p0-p4.md)：保留早期已完成能力的详细背景。

## 当前候选池摘要

### 早期遗留候选

7 项：P6-74、P6-76、P6-77、P6-80、P6-84、P6-85、P6-86

### 扩展候选池 P6-94 到 P6-128

26 项：P6-95、P6-96、P6-98、P6-101、P6-102、P6-103、P6-104、P6-106、P6-107、P6-108、P6-109、P6-110、P6-111、P6-112、P6-113、P6-116、P6-117、P6-118、P6-119、P6-120、P6-121、P6-122、P6-123、P6-124、P6-126、P6-127

### 扩展候选池 P6-129 到 P6-176

41 项：P6-129、P6-130、P6-131、P6-133、P6-134、P6-135、P6-136、P6-137、P6-138、P6-139、P6-140、P6-141、P6-142、P6-143、P6-144、P6-145、P6-146、P6-149、P6-150、P6-151、P6-153、P6-154、P6-155、P6-156、P6-157、P6-158、P6-159、P6-160、P6-161、P6-162、P6-163、P6-164、P6-165、P6-166、P6-167、P6-168、P6-171、P6-173、P6-174、P6-175、P6-176

### 新近候选 P6-184 到 P6-185

2 项：P6-184、P6-185
