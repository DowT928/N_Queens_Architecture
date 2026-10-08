/* ============================================================
 * 文件：Reducer.java
 * 本文件实现的架构风格：MapReduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：Reduce 阶段组件
 *   - 连接器：输入 Shuffle 分组结果，输出 ReduceResult
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 *   [是] 本文件只消费 LEGAL 组，冲突组不进入下一轮 frontier：是
 *   [是] 本文件按 CandidateRecord.order 恢复公共首解顺序：是
 *   [是] 本文件遇到首个完整状态即返回首解：是
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：ChatGPT/Codex；用于任务：语法排错、概念/API 查询、代码审查和文字润色；
 *   自行修改内容：本人负责主要架构组织、框架实现、代码修改和运行验证；声明人（手写签名）：江婧怡
 * ============================================================ */
package nqueens.architectures.mapreduce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class Reducer {
    ReduceResult reduce(Map<CandidateKey, List<CandidateRecord>> grouped, int n) {
        List<CandidateRecord> legalRecords = new ArrayList<>(grouped.get(CandidateKey.LEGAL));
        legalRecords.sort(Comparator.comparingLong(CandidateRecord::order));

        List<SearchState> nextFrontier = new ArrayList<>(legalRecords.size());
        for (CandidateRecord record : legalRecords) {
            SearchState state = record.nextState();
            if (state.row() == n) {
                return ReduceResult.found(state);
            }
            nextFrontier.add(state);
        }
        return ReduceResult.continueWith(nextFrontier);
    }
}
