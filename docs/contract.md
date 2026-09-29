# 公共接口规范

CLI 参数为 `--architecture <id> --n <1..15>`，顺序固定，脚本自动组装。五种标识见根 README。Java CLI 参数错误退出 2；PowerShell 参数绑定错误的宿主退出码可能为 1。

`Solver.solve(int n)` 返回 `new SolveResult(found, columns)`。有解时数组长度为 N，按行记录零基列下标；无解时 false 加空数组。范围内仅 N=2、3 无解。结果构造和读取均复制数组。

stdout 仅一行 JSON，字段为 protocol_version（1）、architecture、n、mode（first）、status、columns、solve_elapsed_ns。status 为 solved、no_solution、not_implemented、error。错误说明写 stderr；参数错误不输出 JSON。
退出码为 0 成功或正确无解、1 异常或非法结果、2 参数错误、3 尚未实现。未实现抛 UnsupportedOperationException，其他失败用其他异常类型。

所有实现按行递增、列从小到大枚举并返回该顺序首解；用三个 long 位向量分别表示列占用、row-col+n-1 和 row+col 对角线占用。禁止对称性优化、答案缓存或不同剪枝。各架构独立实现等价规则，公共验证器不参与搜索。并行实现也必须保持确定的首解顺序。

构造函数不做求解或资源初始化。可变状态、架构初始化、调度、通信、搜索、资源释放和等待线程结束均在 solve 内完成。公共层用 System.nanoTime 包围 solve，排除验证、打印和 JVM 启动。错误状态的耗时不用于分析；首解耗时很短，不应把单次极小差值当作稳定结论。

成员在自己的目录增加组件、替换占位，不更改公共契约；填写自查、AI 声明和实现状态。按任务书先完成管道和黑板审核，再推进其他架构。最终机器及 JDK 一致性由小组核对，脚本记录环境但不代替跨批次比较。
