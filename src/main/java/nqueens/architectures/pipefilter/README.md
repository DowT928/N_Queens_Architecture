# 管道-过滤器

## 基本信息

- 负责人：杨铱玫
- 命令标识：`pipefilter`
- 程序入口：`nqueens.architectures.pipefilter.ArchitectureSolver`
- 公共 CLI：`nqueens.cli.Main`
- 求解模式：按行递增、列从小到大进行确定性深度优先搜索，返回第一个解。
- 实现状态：已实现，可通过公共脚本编译并运行。

## 运行环境

- **Java 版本**：JDK 17 ；编译脚本固定使用 `javac --release 17 -encoding UTF-8`，因此生成 Java 17 目标字节码。JDK 目录必须同时包含 `bin/java.exe` 和 `bin/javac.exe`。
- **构建方式**：未使用 Maven 或 Gradle，也没有对应构建文件；`scripts/run.ps1` 通过 `scripts/common.ps1` 直接调用 `javac` 编译 `src/main/java` 下的全部 Java 源文件。
- **操作系统与 Shell**：仓库基准环境为 Windows 和 PowerShell 5.1。脚本使用 Windows 路径、`Get-CimInstance` 和 PowerShell 脚本；本 README 的 N=8 示例已在 PowerShell 7.6.5、JDK 17.0.12 环境实际验证。
- **运行脚本**：日常运行使用 `scripts/run.ps1`。脚本参数 `-Architecture` 和 `-N` 必填，`-JavaHome`、`-JvmArgs` 可选；N 的允许范围为 1～15。
- **JDK 选择顺序**：显式传入的 `-JavaHome` 优先，其次是环境变量 `JAVA_HOME`，最后从 PATH 中查找 `java.exe` 并推导 JDK 目录。
- **执行目录**：以下命令均应在项目根目录 `N_Queens_Architecture` 中执行。脚本会以项目根目录作为编译与 Java 子进程的工作目录。

## 项目结构

下列目录树仅展开与管道-过滤器实现、公共入口和运行脚本直接相关的文件：

```text
N_Queens_Architecture/
├─ src/main/java/nqueens/
│  ├─ cli/
│  │  └─ Main.java                              # 公共 CLI、参数解析与 JSON 输出
│  ├─ contract/
│  │  ├─ Solver.java                            # 公共求解接口
│  │  ├─ SolveResult.java                       # 公共求解结果
│  │  └─ ResultValidator.java                    # 公共结果校验
│  └─ architectures/pipefilter/
│     ├─ ArchitectureSolver.java                # 装配组件、Pipe 与工作线程
│     ├─ InputComponent.java                    # 输入组件
│     ├─ PartialSolutionGenerationFilter.java   # 部分解生成过滤器
│     ├─ LegalityValidationFilter.java          # 合法性校验过滤器
│     ├─ CompleteSolutionCollectionFilter.java  # 完整解收集过滤器
│     ├─ ResultOutputFilter.java                 # 结果输出过滤器
│     ├─ Pipe.java                              # 阻塞队列管道连接器
│     ├─ PipelineProtocol.java                  # 管道消息协议
│     ├─ SearchState.java                       # 不可变搜索状态
│     └─ README.md
├─ scripts/
│  ├─ run.ps1                                  # 日常编译与单次运行入口
│  ├─ common.ps1                               # JDK 解析、编译、运行与日志采集
│  └─ ProcessCapture.cs                        # 子进程输出采集
└─ tests/
   └─ PipeFilterChecks.java                    # 管道-过滤器专项检查
```

## 组件与连接器

### 组件

- `ArchitectureSolver`：实现公共 `Solver` 接口；在一次 `solve(int n)` 调用中创建五条 Pipe、四线程执行器并装配各组件，等待求解结束后回收线程资源。
- `InputComponent`：接收 N，通过初始状态管道写入空棋盘 `SearchState`。它由 `ArchitectureSolver` 在调用线程中触发，不直接调用任何过滤器。
- `PartialSolutionGenerationFilter`：从初始状态管道取得初始状态，从反馈管道取得未完成但合法的子状态；按列从小到大生成候选。其私有栈维持深度优先搜索顺序。
- `LegalityValidationFilter`：检查列、主对角线索引 `row-col+n-1`、副对角线索引 `row+col` 的位占用情况，丢弃冲突候选并向下游发送合法状态。
- `CompleteSolutionCollectionFilter`：按批次收集合法状态；完整状态发送到完整解管道，未完成状态通过反馈管道返回生成过滤器；搜索耗尽时发送无解消息。
- `ResultOutputFilter`：从完整解管道读取有解、无解或失败消息并转换为公共 `SolveResult`。它不直接写 stdout；最终单行 JSON 由公共 CLI `Main` 输出。

### 五条 Pipe

`Pipe<T>` 使用 `LinkedBlockingQueue<T>` 封装阻塞式 `put`/`take`。五条 Pipe 的连接关系如下：

| Pipe 变量 | 消息类型 | 上游 | 下游 | 用途 |
|---|---|---|---|---|
| `initialStatePipe` | `SearchState` | `InputComponent` | `PartialSolutionGenerationFilter` | 传递空棋盘初始状态 |
| `candidatePipe` | `CandidateEvent` | `PartialSolutionGenerationFilter` | `LegalityValidationFilter` | 传递候选位置、批次结束、搜索结束或失败消息 |
| `legalStatePipe` | `LegalEvent` | `LegalityValidationFilter` | `CompleteSolutionCollectionFilter` | 传递合法状态及对应的控制消息 |
| `feedbackPipe` | `FeedbackEvent` | `CompleteSolutionCollectionFilter` | `PartialSolutionGenerationFilter` | 反馈未完成合法状态批次或停止消息 |
| `completeSolutionPipe` | `ResultEvent` | `CompleteSolutionCollectionFilter` | `ResultOutputFilter` | 传递首解、无解或失败消息 |

生成过滤器一次发送一个父状态的全部候选及批次结束标记。收集过滤器在收到批次结束标记后，将未完成合法子状态列表经反馈 Pipe 返回；生成过滤器把该列表逆序压栈，使下一次仍从最小列开始深度优先搜索，因此线程调度不会改变首解。

## 运行流程 / 数据流

主数据流如下：

```text
InputComponent
  → initialStatePipe
  → PartialSolutionGenerationFilter
  → candidatePipe
  → LegalityValidationFilter
  → legalStatePipe
  → CompleteSolutionCollectionFilter
  → completeSolutionPipe
  → ResultOutputFilter
  → SolveResult
  → 公共 CLI Main 输出单行 JSON
```

未完成但合法的搜索状态通过反馈 Pipe 继续搜索：

```text
CompleteSolutionCollectionFilter
  → feedbackPipe（ChildrenBatch）
  → PartialSolutionGenerationFilter 的私有 DFS 栈
  → 生成下一批候选
```

当收集过滤器发现本批次中的第一个完整解时，它向 `completeSolutionPipe` 发送 `Solved`，同时向 `feedbackPipe` 发送 `FeedbackStop`。生成过滤器随后通过 `candidatePipe` 发出停止消息，校验过滤器再通过 `legalStatePipe` 转发停止消息，各工作线程有序退出。若私有栈耗尽，则搜索结束消息沿候选 Pipe 和合法状态 Pipe 向下游传播，最终输出 `NoSolution`。

## 实现约束

- 过滤器之间仅通过五条 Pipe/队列传递数据，不共享搜索栈或棋盘数组，也不直接调用其他过滤器的内部方法。
- `ArchitectureSolver` 的构造过程不启动任务；Pipe、执行器、组件和工作任务均在每次 `solve(int n)` 调用中创建。
- 执行器使用固定的四个工作线程，分别运行生成、校验、收集和结果输出组件。正常结束时调用 `shutdown`，异常结束时取消任务并调用 `shutdownNow`，最后等待线程终止。
- `SearchState` 的字段为私有 `final`，数组在构造和读取时均进行复制；搜索数据消息中的数组和列表也进行防御性复制。异常消息会携带 `Throwable` 引用，但该引用不承载或共享可变搜索状态。
- 搜索按行递增、列从小到大枚举，采用列占用和两条对角线占用的三个 `long` 位向量剪枝；合法子状态逆序压栈以维持确定的深度优先首解顺序。
- 不使用对称性优化、预计算答案、跨次缓存或其他会改变跨架构比较公平性的优化。
- 求解器只返回公共 `SolveResult`；stdout 的协议化 JSON 输出由公共 CLI 负责，错误和诊断信息写入 stderr。

## 运行方法

### 1. 准备 JDK

如果已设置 `JAVA_HOME`，可直接运行脚本；也可以在命令中显式传入 JDK 路径：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
```

### 2. 在项目根目录运行

脚本会先把全部 Java 源文件编译到 `build` 下的唯一临时目录，再启动公共 CLI，因此不需要单独执行 Maven、Gradle 或手工编译命令。N=8、10、12 分别运行如下：

```powershell
.\scripts\run.ps1 -Architecture pipefilter -N 8
.\scripts\run.ps1 -Architecture pipefilter -N 10
.\scripts\run.ps1 -Architecture pipefilter -N 12
```

若不使用 `JAVA_HOME`，可以显式指定 JDK：

```powershell
.\scripts\run.ps1 -Architecture pipefilter -N 8 -JavaHome 'C:\path\to\jdk-17'
```

若本机执行策略阻止脚本，可仅对本次进程使用：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Architecture pipefilter -N 8
```

调试运行会将环境、编译命令、stdout、stderr 和退出码保存到 `experiments/debug/pipefilter/<运行编号>/`。编译失败时脚本直接返回，不会运行旧的编译结果。

## 示例运行

以下结果由项目根目录执行 `.\scripts\run.ps1 -Architecture pipefilter -N 8` 实际得到；本次环境为 JDK 17.0.12、PowerShell 7.6.5：

```json
{"protocol_version":1,"architecture":"pipefilter","n":8,"mode":"first","status":"solved","columns":[0, 4, 7, 5, 2, 6, 1, 3],"solve_elapsed_ns":27873600}
```

stdout 的正常结果固定为一行 JSON，字段含义如下：

- `protocol_version`：公共输出协议版本，当前为 `1`。
- `architecture`：架构命令标识，本实现为 `pipefilter`。
- `n`：棋盘规模。
- `mode`：求解模式，当前为 `first`。
- `status`：结果状态；正常求解为 `solved` 或 `no_solution`，运行异常时为 `error`。
- `columns`：按行保存的皇后列下标，列下标从 0 开始；无解或错误时为空数组。
- `solve_elapsed_ns`：本次 `solver.solve(n)` 的实测耗时，单位为纳秒。该字段随机器和运行批次变化，上述单次结果不作为性能结论。

## 架构约束自查

| 检查项 | 结论 | 代码依据 |
|---|---|---|
| 过滤器之间是否仅通过 Pipe / 队列通信 | 是 | 生成、校验、收集、输出组件只持有所需的 `Pipe` 引用；搜索与控制消息均经五条 Pipe 传递。`ArchitectureSolver` 直接调用输入组件的 `publish()` 仅用于注入初始状态，不是过滤器之间的直接调用。 |
| 是否存在过滤器直接调用其他过滤器内部方法 | 否 | 各过滤器分别实现 `Runnable` 或 `Callable`，没有保存其他过滤器实例，也没有调用其他过滤器方法。 |
| 是否共享可变搜索状态 | 否 | DFS 栈仅属于生成过滤器，批次缓冲仅属于收集过滤器；棋盘数组不跨组件共享可变引用。 |
| 消息 / 搜索状态是否不可变 | 是（搜索数据范围） | `SearchState` 对数组进行构造时和读取时复制；`ChildrenBatch` 使用 `List.copyOf`；`Solved` 对数组进行复制。枚举控制消息天然不可变。失败消息持有 `Throwable` 引用，但不含搜索状态。 |
| 线程和队列是否在 `solve` 中创建和回收 | 是 | 五条 `Pipe` 和固定四线程执行器均在 `ArchitectureSolver.solve` 内创建；返回前等待任务，并在 `finally` 中关闭执行器及等待终止。 |
| 搜索剪枝是否使用列占用 + 两条对角线占用 | 是 | `LegalityValidationFilter` 使用 `columnsMask`、`diag1Mask` 和 `diag2Mask`，对应列、`row-col+n-1`、`row+col`。 |
| 是否使用对称性优化、缓存答案或其他跨架构特殊优化 | 否 | 实现逐行逐列生成候选并使用统一的三个位向量剪枝；未发现对称折叠、预计算答案或跨次缓存。 |

## AI 使用说明

各 Java 源文件头部保留架构约束自查及 AI 使用声明，具体 AI 使用记录以源文件注释及课程报告附录为准。

## 相关文档

- [项目总览](../../../../../../README.md)
- [公共接口规范](../../../../../../docs/contract.md)
- [实验与日志说明](../../../../../../docs/experiments.md)
- [自查与 AI 记录模板](../../../../../../docs/templates.md)
