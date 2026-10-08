/*
 * 文件：SearchState.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：不可变搜索状态。
 * 自查：保存 row、columns、三个位向量和 order；按当前行扩展；数组防御性复制。
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
