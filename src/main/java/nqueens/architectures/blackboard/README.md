# 黑板

- 负责人：饶心翊
- 命令标识：`blackboard`
- 入口：`nqueens.architectures.blackboard.ArchitectureSolver`
- 状态：未实现；目前退出码为 3，不是有效实验结果。

## 实现约束

必须包含黑板存储、独立知识源、控制器；知识源只经黑板通信，不直接互相调用。

实现 `SolveResult solve(int n)`，内部组件按自己的架构设计添加。构造函数保持无工作量；全部可变状态、初始化、线程/队列和资源收尾都在 solve 内完成。返回前停止并等待工作线程，不留下后台任务。

按行递增、列从小到大枚举，使用列和两条对角线占用位向量剪枝，返回按此顺序的首解；并行执行也须保持首解确定性。禁止对称性优化、缓存答案或更换剪枝策略。输出交给公共 CLI，组件诊断只能写 stderr。

从项目根目录运行：

```powershell
.\scripts\run.ps1 -Architecture blackboard -N 8
```

参见 [公共接口规范](../../../../../../docs/contract.md) 和 [自查与 AI 记录模板](../../../../../../docs/templates.md)。
