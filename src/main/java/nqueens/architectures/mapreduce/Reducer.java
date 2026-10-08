/*
 * 文件：Reducer.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Reduce 阶段。
 * 自查：只消费 LEGAL 组；按 order 恢复顺序；首个完整状态即返回。
 * AI 使用情况：仅用于语法排错、概念/API 查询、代码审查和文字润色。
 * 本人工作：负责主要架构组织、框架实现、代码修改和运行验证。
 */
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
