/*
 * 文件：Reducer.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：Reduce 阶段组件，从 Shuffle 分组中聚合 LEGAL 记录。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：只消费 LEGAL 组，冲突组不进入下一轮 frontier。
 * 2. 是：按 CandidateRecord.order 排序，恢复公共 row/col 首解顺序。
 * 3. 是：发现 row == N 的状态时返回该顺序下首个完整解。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 MapReduce 的 Reduce 阶段。
 * 自行修改内容：新增合法记录排序、frontier 生成和首解返回逻辑。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
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
