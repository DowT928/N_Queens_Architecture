# N 皇后多架构实验

使用五种软件架构实现 N 皇后问题，统一输入输出和实验流程，对比架构差异。

## 环境与约定

- Windows、PowerShell 5.1、JDK 17 或 21。
- 统一使用 `--release 17 -encoding UTF-8` 编译。
- 求第一个解，按行递增、列从小到大搜索。
- 使用等价的列及两条对角线占用位向量剪枝。
- 正式实验规模为 N=8/10/12，在同一机器、同一 JDK 下运行。

## 目录规划

- `src/main/java/nqueens/cli/`：统一输入输出入口。
- `src/main/java/nqueens/contract/`：公共接口及结果验证。
- `src/main/java/nqueens/architectures/`：五种架构独立实现。
- `scripts/`：编译、调试和实验采集脚本。
- `docs/`：架构约束、分工及记录模板。
- `experiments/debug/`：按架构存放调试数据，不提交 Git。
- `experiments/raw/`：按架构存放正式原始日志，提交 Git。
- `experiments/analysis/`：实验统计与分析。

## 架构与分工

| 架构标识 | 架构 | 负责人 |
|---|---|-----|
| `pipefilter` | 管道-过滤器 | 杨铱玫 |
| `callreturn-iterative` | 调用/返回（迭代） | 戴柔柔 |
| `callreturn-backtracking` | 调用/返回（回溯） | 戴柔柔 |
| `blackboard` | 黑板 | 饶心翊 |
| `mapreduce` | Map-Reduce（单机模拟） | 江婧怡 |

## 开发规范

统一实现接口：

```java
SolveResult solve(int n);
```

返回是否有解及按行排列的列下标数组，下标从 0 开始。公共入口负责结果验证和输出。

各成员在自己的架构目录内开发；公共接口变更须组内同步。源文件按任务书填写架构自查和 AI 使用声明。

## 运行方式

以下为计划提供的命令，脚本尚未实现。实现后在项目根目录执行。

日常调试：

```powershell
.\scripts\run.ps1 -Architecture pipefilter -N 8
```

替换架构标识或 N 即可测试其他实现。输出显示在终端，日志保存至 `experiments/debug/<架构>/<运行编号>/`。

正式实验：

```powershell
.\scripts\experiment.ps1 -Architecture pipefilter -Repeat 5
```

自动运行 N=8/10/12，各重复 5 次，保存至 `experiments/raw/<架构>/<批次编号>/`。

## 原始日志要求

保存完整编译与运行命令、终端输出，以及 OS、CPU、内存、Java/Javac 版本和编译/运行参数；另保留退出码以确认运行结果。

原始日志不得修改或覆盖，失败记录也须保留。重新实验生成新记录，统计分析另存。调试数据不得直接改名作为正式日志。

## 当前状态

项目处于规范设计阶段，公共骨架、运行脚本和五种架构实现均待完成。
