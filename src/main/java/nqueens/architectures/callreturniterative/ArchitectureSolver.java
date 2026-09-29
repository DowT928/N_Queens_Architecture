/*
 * 架构：调用/返回（迭代）；负责人：戴柔柔。
 * 组件/连接器：仅适配入口占位，内部组件待实现。
 * 约束：体现主程序→调度层→求解层→数据表示层；求解层使用循环和显式栈迭代，不使用递归回溯。
 * 架构自查：待负责人实现后逐项填写是/否，当前未实现。
 * AI：Codex 生成占位；本人修改、审核及手写声明：待负责人填写。
 */
package nqueens.architectures.callreturniterative;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        throw new UnsupportedOperationException("callreturn-iterative: 尚未实现");
    }
}
