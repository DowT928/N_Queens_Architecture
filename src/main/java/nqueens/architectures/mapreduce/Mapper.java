/*
 * 文件：Mapper.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：Map 阶段组件，把一个 SearchState 展开为候选记录流。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：按当前 row 枚举 col = 0..N-1，不做启发式排序。
 * 2. 是：只使用列、主对角线、副对角线三个位向量判断冲突。
 * 3. 是：对合法和冲突候选都输出 CandidateRecord，供 Shuffler 真实分组。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 MapReduce 的 Map 阶段。
 * 自行修改内容：新增单状态候选枚举与稳定 order 计算。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
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
