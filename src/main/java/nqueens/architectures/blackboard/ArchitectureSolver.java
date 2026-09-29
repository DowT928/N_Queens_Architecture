/*
 * 架构：黑板；负责人：饶心翊。
 * 组件/连接器：仅适配入口占位，内部组件待实现。
 * 约束：必须包含黑板存储、独立知识源、控制器；知识源只经黑板通信，不直接互相调用。
 * 架构自查：待负责人实现后逐项填写是/否，当前未实现。
 * AI：Codex 生成占位；本人修改、审核及手写声明：待负责人填写。
 */
package nqueens.architectures.blackboard;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        throw new UnsupportedOperationException("blackboard: 尚未实现");
    }
}
