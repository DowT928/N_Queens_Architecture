# 黑板

- 负责人：饶心翊
- 命令标识：`blackboard`
- 入口：`nqueens.architectures.blackboard.ArchitectureSolver`
- 状态：阶段二代码审核已完成；阶段三正式实验按统一脚本采集原始日志。

## 实现约束

必须包含黑板存储、独立知识源、控制器；知识源只经黑板通信，不直接互相调用。

组件划分：`BlackboardStorage` 保存图中 `row`、`candidateCol`、`queens[]`、三个占用位向量、
`status` 和 `result`；`CandidateKnowledgeSource` 生成候选列，`ColumnKnowledgeSource` 与
`DiagonalKnowledgeSource` 组成约束检查，`SolutionKnowledgeSource` 判定候选和完整解；
`Controller` 按图示顺序调度知识源并负责搜索回退。上述组件均拆分为独立 Java 文件，知识源之间
没有直接调用，只通过黑板交换状态。

实现 `SolveResult solve(int n)`，内部组件按自己的架构设计添加。构造函数保持无工作量；全部可变状态、初始化、线程/队列和资源收尾都在 solve 内完成。返回前停止并等待工作线程，不留下后台任务。

按行递增、列从小到大枚举，使用列和两条对角线占用位向量剪枝，返回按此顺序的首解；并行执行也须保持首解确定性。禁止对称性优化、缓存答案或更换剪枝策略。输出交给公共 CLI，组件诊断只能写 stderr。

从项目根目录运行：

```powershell
.\scripts\run.ps1 -Architecture blackboard -N 8
```

## 阶段三正式实验

正式实验固定使用 N=8、N=10、N=12，每个规模独立运行 5 次。提交前确认代码已提交且工作区干净，
再使用同一台机器、同一 JDK 和相同参数运行：

```powershell
.\scripts\experiment.ps1 -Architecture blackboard -Repeat 5 -JavaHome 'D:\JAVA\jdk-17.0.12'
```

脚本会在 `experiments/raw/blackboard/<批次编号>/` 保存环境信息、完整编译命令、每次运行的 stdout、
stderr 和退出码。原始日志不得修改、补写、覆盖或删除；性能分析应另存到 `experiments/analysis/`。
`solve_elapsed_ns` 只表示单次 `solve` 调用耗时，不能根据单次结果下结论。

阶段三提交前应核对 N=8、N=10、N=12 的每次运行均返回退出码 0，状态为 `solved`，并且 `columns` 结果
合法且按统一的行递增、列递增首解规则保持一致。

参见 [公共接口规范](../../../../../../docs/contract.md) 和 [自查与 AI 记录模板](../../../../../../docs/templates.md)。
