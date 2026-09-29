# 实验与日志说明

调试和正式实验共用采集逻辑，按架构隔离，每次调用创建唯一目录。正式采集检查 Git 已有提交且工作区干净，不自动提交；上一批正式日志也需先提交，运行期间不要改代码。

```text
raw/<架构>/<批次>/
  environment.txt
  compile-command.txt
  compile-stdout.log
  compile-stderr.log
  compile-exit-code.txt
  N8-run01/
    command.txt
    stdout.log
    stderr.log
    exit-code.txt
  N10-run01/...
  N12-run01/...
```

公共环境及编译记录适用于该批次每次运行，不重复复制。环境记录 OS、CPU、物理内存、Java/Javac 版本及生效的 Java 环境选项。完整命令用 PowerShell 语法，包含工作目录和全部参数。

stdout/stderr 原始字节分开保存并显示在终端，不合并，因而不承诺还原两条流交错显示的总顺序。采集器异常时额外保存 capture-error.txt，不混入子进程原始输出。

编译失败保留输出且不启动 JVM；运行失败保留输出并继续后续组合，批次最终非零退出。环境查询或目录创建失败会终止，可能留下不完整目录；正式日志不可补写，应重新采集。挂起程序需要人工中断，记录不能视为成功实验。

无额外校验清单、Git 快照或时间统计文件。原始文件不得编辑、删除或覆盖，由协作规则和 Git 历史审查落实；Git 禁用 raw 换行符转换。分析另存 analysis，引用架构、批次和运行编号。

每批新建 build 子目录，避免旧 class。构建产物不提交 Git。复现时在相同源码下重新执行编译命令（必要时先创建对应输出目录），再执行运行命令。本地 debug 和 build 可清理，正式日志保留。
