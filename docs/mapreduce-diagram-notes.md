# MapReduce 架构图说明

负责人：江婧怡  
适用目录：`src/main/java/nqueens/architectures/mapreduce`

## 图文件

- `mapreduce-component-connector.svg`：组件与连接器视图，说明 `ArchitectureSolver`、`MapReduceDriver`、`Mapper`、`Shuffler`、`Reducer`、`SearchState`、`CandidateRecord` 与 `ReduceResult` 的关系。
- `mapreduce-single-round.svg`：单轮 Map → Shuffle → Reduce 数据流，说明一层 frontier 如何扩展为下一层 frontier 或首解。

## 真实 Shuffle 判定标准

本架构不是直接在 Mapper 中返回下一状态，也不是让 Reducer 重新做合法性枚举；Map 阶段必须输出候选记录，Shuffle 阶段必须真实构造按 key 分组的结果。

Shuffle 输出至少包含下列逻辑 key：

- `LEGAL`
- `COLUMN_CONFLICT`
- `DIAG1_CONFLICT`
- `DIAG2_CONFLICT`

Reducer 最终只消费 `LEGAL` 组，但冲突组也必须真实形成，用于证明 Shuffle 不是空壳。冲突记录不进入下一轮 frontier。

## 首解顺序

公共基线要求按 `row = 0..N-1`、`col = 0..N-1` 的顺序返回首个完整解。MapReduce 为避免并行或记录收集顺序改变结果，候选记录需要携带稳定顺序号：

```text
candidateOrder = parentOrder * n + col
```

Reducer 在生成下一轮 frontier 或返回完整解前，必须按该顺序号排序。这样即使后续把 Map 阶段改成多线程，首解定义也仍由公共搜索顺序决定，而不是由线程完成先后决定。

## 与公共契约的关系

公共 CLI 负责输出一行 JSON，字段包括 `protocol_version`、`architecture`、`n`、`mode`、`status`、`columns`、`solve_elapsed_ns`。MapReduce 组件只返回 `SolveResult`，不直接写 stdout。调试信息如有需要只能写 stderr。
