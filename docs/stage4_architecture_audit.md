# 阶段四项目全面审计

> 审计日期：2026-10-11（Asia/Shanghai）  
> 审计范围：当前工作树、当前可见 Git 分支与历史、`src/`、`tests/`、`docs/`、`scripts/`、`experiments/`。  
> 审计性质：只检查，不重构；未修改架构实现、算法和实验原始数据。本次仅新增本审计文档，并修正 `docs/templates.md` 中一句与现状冲突的说明。

## 1. 结论摘要

- 仓库按项目既定口径实现了 5 个可运行版本：管道-过滤器、调用/返回（迭代）、调用/返回（回溯）、黑板、MapReduce（单机模拟）。公共入口为 `nqueens.cli.Main`，各版本入口适配器均为各包内的 `ArchitectureSolver`。
- 五个版本均遵守共同搜索基线：逐行放置、每行按列从小到大枚举、求首解，以列及两条对角线的三个 `long` 位向量进行冲突剪枝，不使用对称性、答案缓存或启发式列排序。
- 正式实验数据完整：5 个架构 × 3 个 N（8、10、12）× 5 次 = 75 次。75 次均退出码 0、状态为 `solved`、stderr 为空，且同一 N 的首解在五个版本之间一致。
- 五个正式批次的环境记录完全相同；每次运行均启动独立 JVM，且每个 JVM 只执行一次 `solve`，没有预热。需要准确表述为“独立 JVM 的首次求解/无预热”；记录的 `solve_elapsed_ns` 不包含 JVM 进程启动、求解器构造、结果校验和 JSON 输出，因此不是完整的 JVM 启动耗时。
- 阶段四派生分析、最终报告、原创反思、可直接使用的 UML、完成版 AI 使用附录和最终提交包均缺失。

### 口径提醒

仓库将“调用/返回（迭代）”和“调用/返回（回溯）”计为两个架构实现。按软件体系结构风格的大类严格分类，它们都属于调用/返回风格，只是搜索控制策略不同。因此仓库有 **5 个实现版本**，但可被理解为 **4 个架构风格大类 + 1 个调用/返回变体**。最终报告应沿用课程任务书的口径，并在文中主动说明该关系，避免被误解为把迭代/回溯本身当作两个完全不同的体系结构风格。

## 2. 五种架构实现审计

公共入口与契约：

- `src/main/java/nqueens/cli/Main.java`：解析 `--architecture` 与 `--n`，选择实现、计时、校验并输出一行 JSON。
- `src/main/java/nqueens/contract/Solver.java`：统一 `SolveResult solve(int n)` 接口。
- `src/main/java/nqueens/contract/SolveResult.java`：公共结果对象，对数组做防御性复制。
- `src/main/java/nqueens/contract/ResultValidator.java`：求解后校验，不参与搜索。

| 架构 | 入口类 | 主要代码文件 | 组件 | 连接器 | 控制流 | 数据共享/传递方式 | 搜索/剪枝策略 | 代码映射 | 核心架构约束 |
|---|---|---|---|---|---|---|---|---|---|
| 管道-过滤器（Pipe-Filter） | 公共 `nqueens.cli.Main` → `nqueens.architectures.pipefilter.ArchitectureSolver` | `ArchitectureSolver.java`、`InputComponent.java`、`PartialSolutionGenerationFilter.java`、`LegalityValidationFilter.java`、`CompleteSolutionCollectionFilter.java`、`ResultOutputFilter.java`、`Pipe.java`、`PipelineProtocol.java`、`SearchState.java` | 输入、部分解生成、合法性校验、完整解收集、结果输出；装配器负责线程生命周期 | 5 条 `Pipe<T>`；底层为 `LinkedBlockingQueue`；以 sealed interface/record/enum 消息表达批次、反馈、停止和失败 | 输入注入初始状态；生成 → 校验 → 收集；未完成子状态由反馈 Pipe 返回生成器；完整解送结果过滤器。4 个工作线程并发运行，装配器等待并回收 | `SearchState` 与协议消息跨过滤器传递；数组和列表防御性复制；DFS 栈只属于生成过滤器，收集缓冲只属于收集过滤器 | 生成器用显式栈做确定性 DFS，合法子状态逆序压栈以保持列递增；校验器用 `columnsMask`、`diag1Mask`、`diag2Mask` 剪枝 | 组件装配：`ArchitectureSolver.solve`；候选顺序/栈：`PartialSolutionGenerationFilter`；位向量判断：`LegalityValidationFilter.validate`；反馈/终止：`CompleteSolutionCollectionFilter`；队列：`Pipe` | 过滤器之间不直接调用；只经 Pipe 通信；跨组件搜索状态不可变；线程、队列、失败传播和资源回收均限制在单次 `solve` 内 |
| 调用/返回（迭代） | 公共 `nqueens.cli.Main` → `nqueens.architectures.callreturniterative.ArchitectureSolver` | `ArchitectureSolver.java`、`IterativeSearch.java`、`BoardState.java` | 调度层、迭代求解层、棋盘数据表示层 | 普通同步方法调用与返回 | `ArchitectureSolver.solve` 同步调用 `IterativeSearch.solve`；循环推进行或回退；最终同步返回 `SolveResult` | `BoardState` 是单次求解内的局部可变对象；`nextColumnByRow` 保存各行下一候选列；不跨线程、不跨调用共享 | 显式栈式 DFS；逐行、逐列；`BoardState.isSafe` 用三个位向量剪枝，回退时撤销占用 | 调度：`ArchitectureSolver`；迭代控制：`IterativeSearch`；状态与位运算：`BoardState` | 主程序 → 调度层 → 求解层 → 数据层的同步调用；求解层不得递归；无后台线程、静态共享状态、额外剪枝或答案缓存 |
| 调用/返回（回溯） | 公共 `nqueens.cli.Main` → `nqueens.architectures.callreturnbacktracking.ArchitectureSolver` | `ArchitectureSolver.java`、`BacktrackingSearch.java`、`BoardState.java` | 调度层、递归回溯求解层、棋盘数据表示层 | 普通同步调用/返回与递归调用栈 | `ArchitectureSolver.solve` 同步调用 `BacktrackingSearch.solve`；`searchRow(row)` 递归进入下一行，失败后撤销并返回上一层 | `BacktrackingSearch` 持有本次求解的 `BoardState`；递归帧传递 `row`；所有状态仅属于该求解器实例 | 递归 DFS 回溯；逐行、逐列；`BoardState.isSafe` 用三个位向量剪枝，首个完整解立即沿调用栈返回 | 调度：`ArchitectureSolver`；递归：`BacktrackingSearch.searchRow`；状态/放置/撤销：`BoardState` | 回溯只存在于求解层；组件以同步调用连接；无共享静态状态、线程、额外剪枝或缓存；成功返回时保留首解棋盘 |
| 黑板（Blackboard） | 公共 `nqueens.cli.Main` → `nqueens.architectures.blackboard.ArchitectureSolver` | `ArchitectureSolver.java`、`BlackboardStorage.java`、`Controller.java`、`KnowledgeSource.java`、`CandidateKnowledgeSource.java`、`ColumnKnowledgeSource.java`、`DiagonalKnowledgeSource.java`、`SolutionKnowledgeSource.java` | 黑板存储、控制器、候选知识源、列约束知识源、对角线知识源、解判定知识源 | 共享黑板是知识源的数据连接器；控制器通过统一 `KnowledgeSource.inspect` 触发知识源 | 控制器按候选 → 列检查 → 对角线检查 → 解检查的次序调度；合法则写入黑板并递归下一行，失败时由控制器回退 | `BlackboardStorage` 集中保存 `row/candidateColumn/columns/masks/status/result/cursors`；知识源只读写黑板，不彼此持有或调用 | 控制器实现确定性递归 DFS；候选源逐列生成；列源与对角线源分别基于三个位向量剪枝 | 装配：`ArchitectureSolver`；共享状态：`BlackboardStorage`；调度/回退：`Controller`；独立规则：四个 Knowledge Source | 必须存在黑板、控制器和独立知识源；知识源之间禁止直接调用，只通过黑板交换状态；黑板及其可变状态每次 `solve` 新建 |
| MapReduce（单机模拟） | 公共 `nqueens.cli.Main` → `nqueens.architectures.mapreduce.ArchitectureSolver` | `ArchitectureSolver.java`、`MapReduceDriver.java`、`Mapper.java`、`Shuffler.java`、`Reducer.java`、`SearchState.java`、`CandidateRecord.java`、`CandidateKey.java`、`ReduceResult.java` | Driver、Mapper、Shuffler、Reducer，以及状态/记录/键/归约结果数据对象 | `List<SearchState>` frontier、`List<CandidateRecord>` 记录流、按 `CandidateKey` 分组的 `EnumMap`、`ReduceResult`；同步方法调用 | Driver 逐层循环；每轮对整个 frontier 执行 Map → Shuffle → Reduce；Reducer 对 LEGAL 组按 `order` 排序，生成下一层或返回首个完整解 | `SearchState` 不可变并复制列数组；合法候选通过 `CandidateRecord.nextState` 传递；冲突候选也形成带 key 的记录；无跨线程共享 | 按行的层次/广度展开；每个父状态逐列映射；Mapper 用三个位向量将候选分类剪枝；`order = parentOrder * n + col` 保持字典序首解 | 调度：`MapReduceDriver`；候选映射/剪枝：`Mapper`；真实分组：`Shuffler`；顺序恢复/下一 frontier：`Reducer` | 每一轮必须真实经过 Map、Shuffle、Reduce；Shuffle 保留 LEGAL 与三类冲突组；Reducer 只推进 LEGAL；当前实现为单进程、单线程模拟，不依赖 Hadoop |

### 2.1 公共搜索基线与一致性

代码和 `docs/NQueens_Shared_Baseline_Concise.md`、`docs/contract.md` 的描述一致：

1. 行顺序为 `row = 0 .. N-1`，列顺序为 `col = 0 .. N-1`。
2. 列冲突位为 `1L << col`。
3. 主对角线索引为 `row - col + n - 1`，副对角线索引为 `row + col`。
4. 冲突候选立即剪枝；不使用对称性优化、启发式排序、随机搜索、记忆化或特定 N 的答案。
5. 管道、两种调用/返回和黑板以深度优先方式找到首解；MapReduce 逐层展开后按 `order` 恢复相同字典序。因此搜索组织不同，但最终首解一致。

### 2.2 当前代码验证

2026-10-11 执行现有 `scripts/verify.ps1`：

- Java 17 编译成功；
- 公共契约检查通过；
- 五个架构的 N=8 CLI 调度均成功并返回相同首解；
- 非法架构、越界 N、非数字 N 和缺失参数的退出行为符合契约。

该验证用于确认当前工作树仍可构建，不替代 75 次正式实验，也没有写入或改动 `experiments/raw/`。

## 3. 实验数据审计

### 3.1 文件清单

`experiments/` 当前包含：

| 目录 | 文件情况 | 审计判断 |
|---|---:|---|
| `experiments/raw/` | 326 个文件：160 个 `.log`、165 个 `.txt`、1 个 `.gitkeep` | 正式原始数据完整；无非标准文件 |
| `experiments/debug/` | 27 个文件；仅管道-过滤器的 3 个调试批次，分别为 N=8、10、12 各 1 次，均成功 | 调试记录，不计入正式 75 次 |
| `experiments/analysis/` | 仅 0 字节 `.gitkeep` | 派生分析尚未形成 |

没有发现 `.csv`、独立 `.json`、`.tsv`、`.xlsx` 或其他分析结果文件。JSON 结果以一行文本保存在每次运行的 `stdout.log` 中。

五个正式批次均包含：1 份环境记录、1 份编译命令、编译 stdout/stderr、编译退出码，以及 15 个运行目录；每个运行目录都具备 `command.txt`、`stdout.log`、`stderr.log`、`exit-code.txt` 四个文件。五次编译的退出码均为 0。

### 3.2 15 个组合与实际运行次数

| 架构 | N | 实际次数 | 成功次数 | 首解 `columns` |
|---|---:|---:|---:|---|
| pipefilter | 8 | 5 | 5 | `[0,4,7,5,2,6,1,3]` |
| pipefilter | 10 | 5 | 5 | `[0,2,5,7,9,4,8,1,3,6]` |
| pipefilter | 12 | 5 | 5 | `[0,2,4,7,9,11,5,10,1,6,8,3]` |
| callreturn-iterative | 8 | 5 | 5 | `[0,4,7,5,2,6,1,3]` |
| callreturn-iterative | 10 | 5 | 5 | `[0,2,5,7,9,4,8,1,3,6]` |
| callreturn-iterative | 12 | 5 | 5 | `[0,2,4,7,9,11,5,10,1,6,8,3]` |
| callreturn-backtracking | 8 | 5 | 5 | `[0,4,7,5,2,6,1,3]` |
| callreturn-backtracking | 10 | 5 | 5 | `[0,2,5,7,9,4,8,1,3,6]` |
| callreturn-backtracking | 12 | 5 | 5 | `[0,2,4,7,9,11,5,10,1,6,8,3]` |
| blackboard | 8 | 5 | 5 | `[0,4,7,5,2,6,1,3]` |
| blackboard | 10 | 5 | 5 | `[0,2,5,7,9,4,8,1,3,6]` |
| blackboard | 12 | 5 | 5 | `[0,2,4,7,9,11,5,10,1,6,8,3]` |
| mapreduce | 8 | 5 | 5 | `[0,4,7,5,2,6,1,3]` |
| mapreduce | 10 | 5 | 5 | `[0,2,5,7,9,4,8,1,3,6]` |
| mapreduce | 12 | 5 | 5 | `[0,2,4,7,9,11,5,10,1,6,8,3]` |

汇总：

- 架构数：5；
- N 取值：8、10、12；
- 组合数：15；
- 每组合：5 次；
- 总运行数：75；
- 完整四文件运行目录：75/75；
- 退出码 0：75/75；
- JSON 可解析：75/75；
- `status=solved`：75/75；
- stderr 为空：75/75；
- 首解一致：是，同一 N 的 25 次结果完全一致。

### 3.3 环境一致性

五份 `environment.txt` 的 SHA-256 均为：

```text
B73C47741911B8992CF5E8D2EF46ED34F3A7D42B239AB039CD66F75967A92CDE
```

记录的环境为：

- Windows 11 家庭版中文版，10.0.26200，64 位；
- 13th Gen Intel Core i9-13900H；
- 16,786,370,560 字节物理内存；
- Java/Javac 17.0.12；
- 五次编译均使用 `--release 17 -encoding UTF-8`；
- 75 条运行命令均未附加额外 JVM 参数。

因此，从已记录的机器、OS、CPU、内存、JDK、编译参数和 JVM 参数看，测试环境一致。环境文件不能证明后台负载、温度、功耗策略等运行时条件完全相同；最终报告不应把 5 次小样本的细微差异表述为稳定性能结论。

### 3.4 独立 JVM、首次求解与“冷启动”口径

审计 75 个 `command.txt` 后确认，每个文件都单独执行一次 `java.exe -cp ... nqueens.cli.Main --architecture ... --n ...`。`Main` 每个进程只创建一个求解器并调用一次 `solve(n)`，脚本没有预热循环。因此：

- 独立 JVM：是；
- 每个 JVM 的首次求解：是；
- 无预热/冷态求解实验：是；
- JVM 进程启动耗时是否计入 `solve_elapsed_ns`：否。

准确表述建议为：**“每次采样在独立 JVM 中进行，记录该 JVM 的首次、无预热核心求解耗时。”** 不应把该字段称为“端到端冷启动耗时”，因为计时从 `solver.solve(n)` 前开始，在校验和打印前结束，且求解器对象的构造也发生在计时之前。

## 4. 阶段四材料审计

| 材料 | 当前状态 | 证据与判断 |
|---|---|---|
| 最终报告草稿（Markdown） | 缺失 | 除本审计外，现有 Markdown 为项目说明、契约、实验说明、公共基线、模板和各架构 README；没有最终报告草稿 |
| 最终报告 `.docx` / `.pdf` | 缺失 | 全仓库未发现 `.docx` 或 `.pdf` |
| 原创反思（不少于 500 字） | 缺失 | 未发现标题、文件或正文包含“个人反思/原创反思/反思”的材料 |
| AI 使用说明 | 部分存在 | `docs/templates.md` 有附录表模板；各架构源码头部普遍有 AI 使用声明，但没有完成版统一附录、完整原始提示词记录或最终签名；公共 CLI、契约、脚本和测试仍有 `owner review pending` |
| 过时 AI 描述 | 已做最小修正 | `docs/templates.md` 原有“五种求解算法未实现”，与代码和实验事实冲突；本次改为“五种求解实现现已完成” |
| 当前 UML | 缺失 | 当前工作树没有 `.drawio/.puml/.plantuml/.uml/.svg/.png/.jpg/.vsdx` 等 UML/架构图文件，无法直接用于最终报告 |
| Git 历史中的图 | 有但不可直接使用 | 提交 `ce805f9` 曾加入 `docs/mapreduce-component-connector.svg` 与 `docs/mapreduce-single-round.svg`，随后在 `4672c7d` 删除；仅覆盖 MapReduce，且图中文字包含 “Mapper Pool” 和“多线程”表述，与当前单线程代码不完全一致 |
| 阶段 1～3 压缩包 | 缺失 | 当前树、所有当前可见远端分支和 Git 文件历史均未发现 `.zip/.rar/.7z/.tar/.gz`；无法在仓库内核验阶段 1～3 包 |
| 阶段 4 提交包 | 缺失 | 未发现阶段 4 压缩包或最终交付目录 |

## 5. 缺失内容分级

### 必须补

1. `experiments/analysis/` 的派生分析：至少应有可复核的汇总表（建议 CSV）、每组合的 5 次原值、均值/中位数/离散程度，以及图表和分析方法说明；不得改写 `raw/`。
2. 最终报告草稿及最终 PDF：应整合问题、统一基线、五种架构、组件/连接器、控制流、实验方法、结果、局限和结论。
3. 与当前代码一致的 UML/架构图：至少覆盖五个实现的关键类/组件与连接器；历史 MapReduce SVG 只能作为参考，不能原样当作最终材料。
4. 不少于 500 字的个人原创反思，并明确个人工作、设计取舍、实验认识、问题与改进。
5. 完成版 AI 使用附录：逐项填写工具及版本、任务、原始提示词、关键输出、本人理解与修改；核实源码/脚本中的 `owner review pending` 与签名状态。
6. 阶段 4 最终提交包；如果课程要求同时附阶段 1～3 包，还必须从仓库外定位或重新整理，因为本仓库内不存在这些压缩包。

### 建议补

1. 在最终报告中明确“5 个实现版本”与“调用/返回的两个变体”的分类口径。
2. 在实验分析中明确计时边界：独立 JVM、首次求解、无预热，但 `solve_elapsed_ns` 不包含进程启动和输出；同时披露 5 次样本量较小及系统负载未被记录的限制。
3. 为派生数据保留脚本或计算说明，并在分析表中引用架构、批次 ID、N 和 run 编号，保证结果可追溯。
4. 最终打包前在干净工作树上再次执行验证，并核对 PDF 中的图、表、代码类名与当前实现一致。
5. 评估是否保留 `experiments/debug/`；若保留，应在报告中明确它不是正式数据，避免与 75 次正式运行混淆。

### 可不处理

1. 不需要为了阶段四重构五套架构实现；现有代码已通过公共验证且正式数据完整。
2. 不需要修改搜索/剪枝策略或重跑原始实验，除非后续发现报告口径错误、数据损坏或课程明确要求热身/同 JVM 重复实验。
3. 不需要把 MapReduce 改造成 Hadoop/分布式版本；仓库与说明均明确其为单机模拟。
4. 不需要清理或重排现有目录；当前结构能够支撑报告、分析和打包。

## 6. 推荐后续执行顺序

严格按以下顺序推进，每一步完成后再进入下一步：

1. **实验分析**：从 75 份 `stdout.log` 生成派生表和图，核对统计口径、单位与异常值；原始数据保持只读。
2. **最终报告**：先形成 Markdown 主稿，纳入本审计的架构映射、实验方法、结果、限制和一致性结论，再生成 PDF。
3. **原创反思**：独立撰写不少于 500 字，避免把项目说明或 AI 总结当作个人反思。
4. **AI 附录**：汇总并核实模板、源码头注释、原始提示词、人工修改和签名，清除剩余“待填写/待审核”状态。
5. **最终打包**：加入源码、脚本、文档、UML、派生分析、最终 PDF、反思和 AI 附录；按课程要求决定是否并入阶段 1～3 包；排除 `build/`、IDE 文件和调试噪声。
6. **最终验收**：在解压后的提交包上复查可编译性、五个入口、15 个组合的引用、图表可读性、PDF 完整性、文件命名和包内清单。

本审计到此停止，不执行上述后续任务。
