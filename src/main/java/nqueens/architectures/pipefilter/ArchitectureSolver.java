/*
 * 架构：管道-过滤器；负责人：杨铱玫。
 * 组件/连接器：仅适配入口占位，内部组件待实现。
 * 约束：过滤器仅通过管道/队列传递数据，不共享可变状态，不直接调用彼此内部函数。
 * 架构自查：待负责人实现后逐项填写是/否，当前未实现。
 * AI：Codex 生成占位；本人修改、审核及手写声明：待负责人填写。
 */
package nqueens.architectures.pipefilter;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        throw new UnsupportedOperationException("pipefilter: 尚未实现");
    }
}
