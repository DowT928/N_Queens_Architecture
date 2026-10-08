/* ============================================================
 * 文件：Mapper.java
 * 本文件实现的架构风格：MapReduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：Map 阶段组件
 *   - 连接器：输入 SearchState，输出 CandidateRecord 记录列表
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 *   [是] 本文件按当前 row 枚举 col = 0..N-1：是
 *   [是] 本文件只使用列、主对角线、副对角线三个位向量剪枝：是
 *   [是] 本文件对合法和冲突候选都输出 CandidateRecord：是
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：ChatGPT/Codex；用于任务：语法排错、概念/API 查询、代码审查和文字润色；
 *   自行修改内容：本人负责主要架构组织、框架实现、代码修改和运行验证；声明人（手写签名）：江婧怡
 * ============================================================ */
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
