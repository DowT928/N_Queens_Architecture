# 调用/返回（迭代）

- 负责人：戴柔柔
- 命令标识：`callreturn-iterative`
- 入口：`nqueens.architectures.callreturniterative.ArchitectureSolver`
- 状态：已实现基于显式栈的迭代首解搜索。

## 实现约束

体现主程序→调度层→求解层→数据表示层的同步调用；求解层使用循环和显式栈迭代，不使用递归回溯。

`ArchitectureSolver.solve(int n)` 校验输入范围并调用 `IterativeSearch`。搜索层用 `nextColumnByRow` 保存每行下次尝试的列，以循环推进或回退；`BoardState` 保存皇后位置和列、两条对角线的占用位向量。求解状态在每次 `solve` 内创建，不启动后台线程。

按行递增、列从小到大枚举，使用列和两条对角线占用位向量剪枝，返回按此顺序的首解。禁止对称性优化、缓存答案或更换剪枝策略。输出交给公共 CLI，组件诊断只能写 stderr。

从项目根目录运行：

```powershell
.\scripts\run.ps1 -Architecture callreturn-iterative -N 8
```

参见 [公共接口规范](../../../../../../docs/contract.md) 和 [自查与 AI 记录模板](../../../../../../docs/templates.md)。
