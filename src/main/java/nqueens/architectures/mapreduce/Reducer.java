/*
 * 文件：Reducer.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Reduce 阶段。
 * 自查：只消费 LEGAL 组；按 order 恢复顺序；首个完整状态即返回。
 * AI 使用声明：使用过 AI 辅助，已人工审核修改，详情见报告附录。
 * 声明人：江婧怡（手写签名另附）。
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
