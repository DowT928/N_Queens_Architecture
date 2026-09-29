# N 皇后多架构实验

五种架构实现 N 皇后，统一输入输出和实验采集。**公共骨架已建立，五种架构待实现。** 当前占位入口退出码为 3，不是有效实验结果。

## 环境与约定

Windows、PowerShell 5.1、JDK 17 或 21，无第三方框架；统一用 `--release 17 -encoding UTF-8` 编译。求第一个解，按行递增、列从小到大搜索，采用等价的列及两条对角线占用位向量剪枝。

## 目录

```text
src/main/java/nqueens/
  cli/                         统一入口
  contract/                    接口、结果和验证
  architectures/
    pipefilter/
    callreturniterative/
    callreturnbacktracking/
    blackboard/
    mapreduce/
scripts/                       编译、运行和采集
docs/                          规范与模板
tests/                         公共基础验证
experiments/
  debug/                       自动创建，Git 忽略
  raw/                         正式日志
  analysis/                    派生分析
```

## 分工

| 架构目录及说明 | 命令标识 | 负责人 |
|---|---|---|
| [pipefilter](src/main/java/nqueens/architectures/pipefilter/README.md) | `pipefilter` | 杨铱玫 |
| [callreturniterative](src/main/java/nqueens/architectures/callreturniterative/README.md) | `callreturn-iterative` | 戴柔柔 |
| [callreturnbacktracking](src/main/java/nqueens/architectures/callreturnbacktracking/README.md) | `callreturn-backtracking` | 戴柔柔 |
| [blackboard](src/main/java/nqueens/architectures/blackboard/README.md) | `blackboard` | 饶心翊 |
| [mapreduce](src/main/java/nqueens/architectures/mapreduce/README.md) | `mapreduce` | 江婧怡 |

成员在所属目录实现 `SolveResult solve(int n)` 并按架构拆分组件；公共接口、脚本变更须组内同步。结果数组按行保存零基列下标。详见 [接口规范](docs/contract.md)、[自查和 AI 记录模板](docs/templates.md)。

## 日常调试

在项目根目录运行，先设置本机实际 JDK 路径：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\scripts\run.ps1 -Architecture pipefilter -N 8
```

也可传 `-JavaHome '实际 JDK 路径'`；选择顺序为显式参数、JAVA_HOME、PATH。通过 `-JvmArgs @('-Xms128m','-Xmx512m')` 指定 JVM 参数，正式比较时保持一致。

更换架构标识或 N 即可切换。终端显示输出，调试记录保存至 `experiments/debug/<架构>/<运行编号>/`，可清理且不提交 Git。编译失败不会运行旧程序。

若本机策略阻止脚本，可对本次进程使用：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Architecture pipefilter -N 8
```

## 正式实验

先提交全部改动（包括上一批正式日志），在同一机器、同一 JDK、相同参数下运行：

```powershell
.\scripts\experiment.ps1 -Architecture pipefilter -Repeat 5
```

自动运行 N=8/10/12，各 5 次独立 JVM，无隐含预热。保存至 `experiments/raw/<架构>/<批次编号>/`。占位和失败同样留记录，不能作为有效性能数据。

批次保存 OS、CPU、内存、Java/Javac 版本、相关环境选项、完整编译命令及输出；每次运行保存完整命令、原始 stdout/stderr 和退出码。命令包括工作目录与参数。详见 [日志说明](docs/experiments.md)。

**原始日志不得修改、覆盖或删除。** 重跑创建新记录，分析另存；调试数据不得改名充当正式日志。不提交 IDE 配置、构建产物和调试数据。

## 验证

```powershell
.\scripts\verify.ps1 -JavaHome $env:JAVA_HOME
```

验证公共契约及 CLI 行为；不替代架构人工审查。架构完成后验证 N=1 有解、N=2/3 无解，以及 N=8/10/12 首解合法且一致。
