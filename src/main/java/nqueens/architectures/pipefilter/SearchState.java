/*
 * 文件：SearchState.java
 * 架构风格：管道-过滤器。
 * 本文件组件/连接器：过滤器之间传递的不可变搜索状态。
 * 架构约束自查：
 * 1. 是：数组在构造和读取时均复制，无共享可变搜索状态。
 * 2. 是：不调用任何过滤器的内部方法。
 * 3. 是：状态只作为管道消息的数据载荷使用。
 * AI：OpenAI Codex 辅助实现并检查搜索状态的数据完整性；
 * 本人修改：核对 row、columns 和三个位向量字段的含义与取值范围，检查数组构造及读取时的复制行为，并完善状态说明注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

import java.util.Arrays;

final class SearchState {
    private final int n;
    private final int row;
    private final int[] columns;
    private final long columnsMask;
    private final long diag1Mask;
    private final long diag2Mask;

    SearchState(int n, int row, int[] columns,
                long columnsMask, long diag1Mask, long diag2Mask) {
        if (n < 1 || n > 15) throw new IllegalArgumentException("N must be 1..15");
        if (row < 0 || row > n) throw new IllegalArgumentException("row outside board");
        if (columns == null || columns.length != n)
            throw new IllegalArgumentException("columns must have length N");
        this.n = n;
        this.row = row;
        this.columns = columns.clone();
        this.columnsMask = columnsMask;
        this.diag1Mask = diag1Mask;
        this.diag2Mask = diag2Mask;
    }

    static SearchState initial(int n) {
        int[] columns = new int[n];
        Arrays.fill(columns, -1);
        return new SearchState(n, 0, columns, 0L, 0L, 0L);
    }

    int n() { return n; }
    int row() { return row; }
    int[] columns() { return columns.clone(); }
    long columnsMask() { return columnsMask; }
    long diag1Mask() { return diag1Mask; }
    long diag2Mask() { return diag2Mask; }
}
