/*
 * 文件：MapReduceDriver.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：架构驱动器，逐轮连接 Map、Shuffle、Reduce 三阶段。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：frontier 和阶段组件都在 solve 内创建，构造函数无求解工作。
 * 2. 是：每轮都执行 Map → Shuffle → Reduce，不跳过真实分组阶段。
 * 3. 是：返回公共 SolveResult，stdout JSON 仍由公共 CLI 输出。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 MapReduce 单机模拟调度与首解返回。
 * 自行修改内容：新增按层推进 frontier 的求解驱动。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
 */
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
