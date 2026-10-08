/* ============================================================
 * 文件：ArchitectureSolver.java
 * 本文件实现的架构风格：调用/返回（回溯）
 * 本文件承担的核心组件/连接器：
 *   - 组件：调度层（ArchitectureSolver）
 *   - 连接器：Main 经 Solver.solve(int) 同步调用本层；本层同步调用
 *              BacktrackingSearch，并将 SolveResult 返回给 Main
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填是/否，并简述）：
 *   [是] 本层位于主程序与求解层之间并负责调度：检查参数后调用求解层。
 *   [是] 搜索由求解层执行，本层不实现搜索。
 *   [是] 本层不保存跨调用的可变状态，也不输出求解诊断。
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：OpenAI Codex（GPT-6）；用于任务：核对参数范围、调用关系与公共 Solver 契约；
 *   自行修改内容：solve(int) 的参数范围校验，以及对 BacktrackingSearch 的同步调用和结果返回。 声明人：戴柔柔
 * ============================================================
 */
package nqueens.architectures.callreturnbacktracking;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        if (n < 1 || n > 15) {
            throw new IllegalArgumentException("N must be 1..15");
        }

        return new BacktrackingSearch().solve(n);
    }
}
