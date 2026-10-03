# MapReduce（单机模拟）

- 负责人：江婧怡
- 命令标识：`mapreduce`
- 入口：`nqueens.architectures.mapreduce.ArchitectureSolver`
- 状态：已实现单线程 MapReduce 首解版本；`N=1/2/3/8/10/12` 已通过调试验证。

## 架构边界

本目录实现单进程内的 Map → Shuffle → Reduce 搜索组织方式，不部署 Hadoop，也不依赖第三方框架。公共 CLI 负责参数解析、计时、结果校验和 JSON 输出；本架构只实现 `SolveResult solve(int n)`。

MapReduce 内部组件之间通过记录流和分组结果连接：

1. `MapReduceDriver` 维护当前 `frontier`，按 row 逐轮调度。
2. `Mapper` 读取一个不可变 `SearchState`，枚举当前行 `col = 0..N-1`，输出 `CandidateRecord`。
3. `Shuffler` 必须真实按 `CandidateKey` 分组，形成 `LEGAL`、`COLUMN_CONFLICT`、`DIAG1_CONFLICT`、`DIAG2_CONFLICT` 四类记录。
4. `Reducer` 只消费 `LEGAL` 组，按统一顺序生成下一轮 `frontier`，或返回首个完整解。

组件图见 [mapreduce-component-connector.svg](../../../../../../docs/mapreduce-component-connector.svg)，单轮流程图见 [mapreduce-single-round.svg](../../../../../../docs/mapreduce-single-round.svg)，图注和判定标准见 [mapreduce-diagram-notes.md](../../../../../../docs/mapreduce-diagram-notes.md)。

## 搜索约束

本实现必须与公共基线保持等价：

- 按行搜索 `row = 0..N-1`。
- 每行按列从小到大枚举 `col = 0..N-1`。
- 使用 `columnsMask`、`diag1Mask`、`diag2Mask` 三个位向量剪枝。
- 返回上述顺序下的首个完整解。
- 不使用对称性剪枝、启发式排序、缓存答案或特定 N 的特殊优化。

`SearchState` 保存已放置行数、列数组和三个位向量。候选记录携带 `order`，推荐计算方式为：

```text
candidateOrder = parentOrder * n + col
```

Reducer 必须按 `order` 恢复顺序，因此未来即使 Map 阶段改为并行执行，也不能由线程完成先后决定首解。

## 实现类结构

```text
ArchitectureSolver
  -> MapReduceDriver
       -> Mapper
       -> Shuffler
       -> Reducer

SearchState
CandidateRecord
CandidateKey
ReduceResult
```

构造函数不做求解或资源初始化。全部可变状态、调度、记录集合和资源收尾都在 `solve` 调用链内完成。组件诊断如需输出只能写 stderr，stdout 保留给公共 CLI 的一行 JSON。

当前实现为单线程版本。`MapReduceDriver` 每一轮先收集完整候选记录流，再交给 `Shuffler` 构造四类分组，最后由 `Reducer` 对 `LEGAL` 组按 `order` 排序并生成下一轮 frontier 或首解。冲突组虽然不进入下一轮，但会在 Shuffle 结果中真实存在。

## 调试命令

从项目根目录运行：

```powershell
.\scripts\run.ps1 -Architecture mapreduce -N 8
```

实现完成后至少验证 `N=1/2/3/8/10/12`。标准首解见公共基线文档。

参见 [公共接口规范](../../../../../../docs/contract.md) 和 [自查与 AI 记录模板](../../../../../../docs/templates.md)。
