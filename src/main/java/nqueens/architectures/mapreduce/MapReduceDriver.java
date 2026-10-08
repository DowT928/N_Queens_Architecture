/*
 * 文件：MapReduceDriver.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：阶段调度器。
 * 自查：solve 内创建状态；每轮执行 Map、Shuffle、Reduce；返回公共 SolveResult。
 * AI 使用情况：仅用于语法排错、概念/API 查询、代码审查和文字润色。
 * 本人工作：负责主要架构组织、框架实现、代码修改和运行验证。
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
