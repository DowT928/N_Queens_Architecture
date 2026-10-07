/* ============================================================
 * 文件：IterativeSearch.java
 * 本文件实现的架构风格：调用/返回（迭代）
 * 本文件承担的核心组件/连接器：
 *   - 组件：求解层（IterativeSearch）
 *   - 连接器：由 ArchitectureSolver 同步调用；调用 BoardState 检查、
 *              放置和撤销皇后，并将首解或无解结果返回给调度层
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填是/否，并简述）：
 *   [是] 按行、按列递增枚举，找到首个完整解后立即返回。
 *   [是] 使用循环和 nextColumnByRow 显式栈保存进度，不进行递归调用。
 *   [是] 冲突检查委托 BoardState 的列及两条对角线位向量。
 *   [是] 只返回首解，不统计全部解，也未加入额外剪枝或缓存。
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：OpenAI Codex（GPT-6）； 用于任务：显式行列搜索循环、每行下一列游标、回退时的棋盘状态撤销，以及首解/无解结果构造。
 *   自行修改内容：调用 BoardState初始化棋盘、核对按行按列枚举顺序、显式栈迭代要求和首解行为； 声明人：戴柔柔
 * ============================================================
 */
package nqueens.architectures.callreturniterative;

import nqueens.contract.SolveResult;

final class IterativeSearch {
    SolveResult solve(int n) {
        // 初始化棋盘；游标记录每一行下次尝试的列，代替递归调用栈。
        BoardState board = new BoardState();
        board.initialize(n);
        int[] nextColumnByRow = new int[n];
        int row = 0;

        while (row >= 0) {
            // 从当前行保存的位置开始，按列递增寻找安全位置。
            int col = nextColumnByRow[row];
            while (col < n && !board.isSafe(row, col)) {
                col++;
            }

            if (col < n) {
                // 记录下一次尝试位置并放置皇后；若到达末行则得到首解。
                nextColumnByRow[row] = col + 1;
                board.place(row, col);
                if (row == n - 1) {
                    return new SolveResult(true, board.copyColumns());
                }
                row++;
                nextColumnByRow[row] = 0;
            } else {
                // 当前行无可用列，回到上一行并撤销上一枚皇后。
                nextColumnByRow[row] = 0;
                row--;
                if (row >= 0) {
                    board.remove(row, board.columnAt(row));
                }
            }
        }

        // 所有行均尝试完毕，棋盘无解。
        return new SolveResult(false, new int[0]);
    }
}
