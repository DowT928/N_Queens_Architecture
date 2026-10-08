/* ============================================================
 * 文件：BacktrackingSearch.java
 * 本文件实现的架构风格：调用/返回（回溯）
 * 本文件承担的核心组件/连接器：
 *   - 组件：求解层（BacktrackingSearch）
 *   - 连接器：由 ArchitectureSolver 同步调用；递归尝试每行的列，调用
 *              BoardState 检查、放置和撤销皇后，并返回首解或无解结果
 * ------------------------------------------------------------
 * 架构约束自查清单：
 *   [是] 按行递增、每行按列递增枚举，找到首个完整解后立即返回。
 *   [是] 递归回溯仅位于求解层，不把回溯作为独立架构风格。
 *   [是] 冲突检查委托 BoardState 的列及两条对角线位向量。
 *   [是] 只返回首解，不统计全部解，也未加入额外剪枝或缓存。
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：OpenAI Codex（GPT-6）；用于任务：实现递归回溯搜索及按顺序返回首解。
 *   自行修改内容：调用 BoardState初始化棋盘、核对按行按列枚举顺序、回溯要求和首解行为；声明人：戴柔柔
 * ============================================================
 */
package nqueens.architectures.callreturnbacktracking;

import nqueens.contract.SolveResult;

final class BacktrackingSearch {
    private BoardState board;
    private int n;

    // 递归回溯搜索，找到首解或无解。
    SolveResult solve(int size) {
        n = size;
        board = new BoardState();
        board.initialize(n);
        // 从第一行开始递归搜索。
        boolean found = searchRow(0);
        return found
                ? new SolveResult(true, board.copyColumns())
                : new SolveResult(false, new int[0]);
    }

    // 递归搜索指定行的皇后位置。
    private boolean searchRow(int row) {
        // 所有行都放置成功，得到一个解。
        if (row == n) {
            return true;
        }

        // 按列从小到大尝试当前位置。
        for (int col = 0; col < n; col++) {
            if (!board.isSafe(row, col)) {
                continue;
            }

            // 放置皇后并递归处理下一行。
            board.place(row, col);
            if (searchRow(row + 1)) {
                return true;
            }
            // 后续无解时撤销选择，继续尝试下一列。
            board.remove(row, col);
        }
        return false;
    }
}
