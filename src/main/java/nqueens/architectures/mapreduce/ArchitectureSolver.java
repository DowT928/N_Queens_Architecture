/*
 * 文件：ArchitectureSolver.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：公共 Solver 适配入口，连接公共 CLI 与 MapReduceDriver。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：构造函数不做求解或资源初始化，solve 内创建 Driver 并返回结果。
 * 2. 是：不直接枚举棋盘候选，搜索组织委托给 MapReduceDriver。
 * 3. 是：不写 stdout，统一输出由公共 CLI 负责。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 MapReduce 首解版本入口适配。
 * 自行修改内容：替换未实现占位，调用真实 MapReduce 单机模拟求解器。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
 */
package nqueens.architectures.mapreduce;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        return new MapReduceDriver().solve(n);
    }
}
