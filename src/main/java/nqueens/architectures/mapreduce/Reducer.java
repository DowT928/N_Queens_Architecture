/*
 * 文件：Reducer.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Reduce 阶段。
 * 自查：只消费 LEGAL 组；按 order 恢复顺序；首个完整状态即返回。
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
