/*
 * 文件：Mapper.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Map 阶段。
 * 自查：按列顺序枚举；只用三个位向量剪枝；合法和冲突候选都输出记录。
 * AI 使用情况：仅用于语法排错、概念/API 查询、代码审查和文字润色。
 * 本人工作：负责主要架构组织、框架实现、代码修改和运行验证。
 */
package nqueens.architectures.mapreduce;

import java.util.ArrayList;
import java.util.List;

final class Mapper {
    List<CandidateRecord> map(SearchState state, int n) {
        List<CandidateRecord> records = new ArrayList<>(n);
        int row = state.row();
        for (int col = 0; col < n; col++) {
            long order = state.order() * n + col;
            CandidateKey key = classify(state, n, row, col);
            SearchState nextState = key == CandidateKey.LEGAL ? state.place(n, col, order) : null;
            records.add(new CandidateRecord(key, row, col, order, nextState));
        }
        return records;
    }

    private CandidateKey classify(SearchState state, int n, int row, int col) {
        long columnBit = 1L << col;
        if ((state.columnsMask() & columnBit) != 0L) {
            return CandidateKey.COLUMN_CONFLICT;
        }

        long diag1Bit = 1L << (row - col + n - 1);
        if ((state.diag1Mask() & diag1Bit) != 0L) {
            return CandidateKey.DIAG1_CONFLICT;
        }

        long diag2Bit = 1L << (row + col);
        if ((state.diag2Mask() & diag2Bit) != 0L) {
            return CandidateKey.DIAG2_CONFLICT;
        }

        return CandidateKey.LEGAL;
    }
}
