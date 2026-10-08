/*
 * 文件：ReduceResult.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Reduce 输出结果。
 * 自查：区分下一轮 frontier 和首解；不参与搜索；返回结果时复制数组。
 */
package nqueens.architectures.mapreduce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ReduceResult {
    private final List<SearchState> nextFrontier;
    private final SearchState firstSolution;

    private ReduceResult(List<SearchState> nextFrontier, SearchState firstSolution) {
        this.nextFrontier = Collections.unmodifiableList(new ArrayList<>(nextFrontier));
        this.firstSolution = firstSolution;
    }

    static ReduceResult continueWith(List<SearchState> nextFrontier) {
        return new ReduceResult(nextFrontier, null);
    }

    static ReduceResult found(SearchState firstSolution) {
        return new ReduceResult(List.of(), firstSolution);
    }

    boolean hasFirstSolution() {
        return firstSolution != null;
    }

    int[] firstSolutionColumns() {
        if (firstSolution == null) {
            throw new IllegalStateException("no first solution available");
        }
        return firstSolution.columns();
    }

    List<SearchState> nextFrontier() {
        return nextFrontier;
    }
}
