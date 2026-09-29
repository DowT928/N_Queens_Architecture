/*
 * 架构：调用/返回（回溯）；负责人：戴柔柔。
 * 组件/连接器：仅适配入口占位，内部组件待实现。
 * 约束：体现分层调用；递归回溯放在求解层内部，回溯不是独立架构风格。
 * 架构自查：待负责人实现后逐项填写是/否，当前未实现。
 * AI：Codex 生成占位；本人修改、审核及手写声明：待负责人填写。
 */
package nqueens.architectures.callreturnbacktracking;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        throw new UnsupportedOperationException("callreturn-backtracking: 尚未实现");
    }
}
