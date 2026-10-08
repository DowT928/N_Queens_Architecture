/* ============================================================
 * 文件：SearchState.java
 * 本文件实现的架构风格：MapReduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：不可变搜索状态
 *   - 连接器：作为 Mapper 输入和 CandidateRecord 中的合法下一状态
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 *   [是] 本文件保存 row、columns、三个位向量和 order：是
 *   [是] 本文件 place 只扩展当前 row 的一个候选列：是
 *   [是] 本文件对 columns 做防御性复制，避免共享可变数组：是
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：ChatGPT/Codex；用于任务：语法排错、概念/API 查询、代码审查和文字润色；
 *   自行修改内容：本人负责主要架构组织、框架实现、代码修改和运行验证；声明人（手写签名）：江婧怡
 * ============================================================ */
package nqueens.architectures.mapreduce;

import java.util.Arrays;

final class SearchState {
    private final int row;
    private final int[] columns;
    private final long columnsMask;
    private final long diag1Mask;
    private final long diag2Mask;
    private final long order;

    private SearchState(int row, int[] columns, long columnsMask, long diag1Mask, long diag2Mask, long order) {
        this.row = row;
        this.columns = columns.clone();
        this.columnsMask = columnsMask;
        this.diag1Mask = diag1Mask;
        this.diag2Mask = diag2Mask;
        this.order = order;
    }

    static SearchState root() {
        return new SearchState(0, new int[0], 0L, 0L, 0L, 0L);
    }

    SearchState place(int n, int col, long nextOrder) {
        int[] nextColumns = Arrays.copyOf(columns, row + 1);
        nextColumns[row] = col;
        long columnBit = 1L << col;
        long diag1Bit = 1L << (row - col + n - 1);
        long diag2Bit = 1L << (row + col);
        return new SearchState(
                row + 1,
                nextColumns,
                columnsMask | columnBit,
                diag1Mask | diag1Bit,
                diag2Mask | diag2Bit,
                nextOrder);
    }

    int row() {
        return row;
    }

    int[] columns() {
        return columns.clone();
    }

    long columnsMask() {
        return columnsMask;
    }

    long diag1Mask() {
        return diag1Mask;
    }

    long diag2Mask() {
        return diag2Mask;
    }

    long order() {
        return order;
    }
}
