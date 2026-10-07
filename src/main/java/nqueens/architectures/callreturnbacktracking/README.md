# 调用/返回（回溯）

- 负责人：戴柔柔
- 命令标识：`callreturn-backtracking`
- 入口：`nqueens.architectures.callreturnbacktracking.ArchitectureSolver`
- 状态：已实现递归回溯首解搜索。

## 实现约束

体现主程序→调度层→求解层→数据表示层的同步调用；递归回溯放在求解层内部，回溯不是独立架构风格。

`ArchitectureSolver.solve(int n)` 校验输入范围并调用 `BacktrackingSearch`。搜索层从第 0 行开始递归，按列递增尝试；失败时撤销棋盘状态并回到上一层，成功后保留首解。`BoardState` 保存列及两条对角线位向量。求解状态在每次 `solve` 内创建；不启动后台线程。

按行递增、列从小到大枚举，使用列和两条对角线占用位向量剪枝，返回按此顺序的首解。禁止对称性优化、缓存答案或更换剪枝策略。输出交给公共 CLI，组件诊断只能写 stderr。

从项目根目录运行：

```powershell
.\scripts\run.ps1 -Architecture callreturn-backtracking -N 8
```

参见 [公共接口规范](../../../../../../docs/contract.md) 和 [自查与 AI 记录模板](../../../../../../docs/templates.md)。
