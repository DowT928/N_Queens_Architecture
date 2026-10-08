/* ============================================================
 * 文件：MapReduceDriver.java
 * 本文件实现的架构风格：MapReduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：MapReduce 阶段调度器
 *   - 连接器：按轮连接 Mapper、Shuffler、Reducer
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 *   [是] 本文件在 solve 内创建 frontier 和阶段组件：是
 *   [是] 本文件每轮都执行 Map → Shuffle → Reduce：是
 *   [是] 本文件返回 SolveResult，不直接负责 JSON 输出：是
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：ChatGPT/Codex；用于任务：语法排错、概念/API 查询、代码审查和文字润色；
 *   自行修改内容：本人负责主要架构组织、框架实现、代码修改和运行验证；声明人（手写签名）：江婧怡
 * ============================================================ */
package nqueens.architectures.mapreduce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nqueens.contract.SolveResult;

final class MapReduceDriver {
    SolveResult solve(int n) {
        if (n < 1 || n > 15) {
            throw new IllegalArgumentException("N must be 1..15");
        }

        Mapper mapper = new Mapper();
        Shuffler shuffler = new Shuffler();
        Reducer reducer = new Reducer();
        List<SearchState> frontier = new ArrayList<>();
        frontier.add(SearchState.root());

        while (!frontier.isEmpty()) {
            List<CandidateRecord> mapped = mapFrontier(mapper, frontier, n);
            Map<CandidateKey, List<CandidateRecord>> grouped = shuffler.shuffle(mapped);
            ReduceResult reduced = reducer.reduce(grouped, n);
            if (reduced.hasFirstSolution()) {
                return new SolveResult(true, reduced.firstSolutionColumns());
            }
            frontier = reduced.nextFrontier();
        }

        return new SolveResult(false, new int[0]);
    }

    private List<CandidateRecord> mapFrontier(Mapper mapper, List<SearchState> frontier, int n) {
        List<CandidateRecord> mapped = new ArrayList<>(frontier.size() * n);
        for (SearchState state : frontier) {
            mapped.addAll(mapper.map(state, n));
        }
        return mapped;
    }
}
