/*
 * 文件：SearchState.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：不可变搜索状态，作为 MapReduce 记录中的领域数据。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：保存 row、columns、columnsMask、diag1Mask、diag2Mask 和稳定 order。
 * 2. 是：place 只添加当前 row 的一个 col，保持按行搜索。
 * 3. 是：columns 输入输出都复制，避免阶段之间共享可变数组。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 MapReduce 搜索状态表示。
 * 自行修改内容：新增不可变状态和位向量更新逻辑。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
 */
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
