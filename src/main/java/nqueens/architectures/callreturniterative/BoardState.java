/* ============================================================
 * 文件：BoardState.java
 * 本文件实现的架构风格：调用/返回（迭代）
 * 本文件承担的核心组件/连接器：
 *   - 组件：数据表示层（BoardState）
 *   - 连接器：由 IterativeSearch 同步调用，提供冲突检查、放置、撤销和结果快照
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填是/否，并简述）：
 *   [是] 保存本次求解的皇后位置和列、两条对角线占用位向量。
 *   [是] 主对角线索引为 row-col+n-1，副对角线索引为 row+col。
 *   [是] 本层不执行搜索、不递归，也不使用静态共享可变状态。
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：OpenAI Codex（GPT-6）； 用于任务：列与两条对角线位向量冲突检查、皇后放置/撤销操作。
 *   自行修改内容：棋盘属性定义、棋盘状态初始化、返回棋盘相关数据； 声明人：戴柔柔
 * ============================================================
 */
package nqueens.architectures.callreturniterative;

import java.util.Arrays;
import java.util.stream.IntStream;

final class BoardState {
    private int n; // 棋盘阶数
    private int[] columns; // 每一行皇后所在的列，-1 表示未放置
    private long columnsMask; // 已占用列
    private long diag1Mask; // 已占用主对角线
    private long diag2Mask; // 已占用副对角线

    // 初始化棋盘及所有占用标记
    void initialize(int size) {
        n = size;
        columns = new int[size];
        Arrays.fill(columns, -1);
        columnsMask = 0L;
        diag1Mask = 0L;
        diag2Mask = 0L;
    }

    // 返回棋盘阶数
    int size() {
        return n;
    }

    // 检查目标列及两条对角线是否均未占用
    boolean isSafe(int row, int col) {
        long columnBit = 1L << col;
        long diag1Bit = 1L << (row - col + n - 1);
        long diag2Bit = 1L << (row + col);
        return (columnsMask & columnBit) == 0L
                && (diag1Mask & diag1Bit) == 0L
                && (diag2Mask & diag2Bit) == 0L;
    }

    // 放置皇后并标记对应列和对角线
    void place(int row, int col) {
        columns[row] = col;
        columnsMask |= 1L << col;
        diag1Mask |= 1L << (row - col + n - 1);
        diag2Mask |= 1L << (row + col);
    }

    // 撤销皇后并清除对应占用标记
    void remove(int row, int col) {
        columns[row] = -1;
        columnsMask &= ~(1L << col);
        diag1Mask &= ~(1L << (row - col + n - 1));
        diag2Mask &= ~(1L << (row + col));
    }

    // 返回指定行的皇后所在列
    int columnAt(int row) {
        return columns[row];
    }

    // 返回棋盘列数据副本，避免暴露内部数组
    int[] copyColumns() {
        return columns.clone();
    }
}
