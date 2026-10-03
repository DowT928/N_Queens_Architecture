/*
 * 文件：ReduceResult.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：Reduce 阶段输出，传回下一轮 frontier 或首解。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：明确区分继续搜索的 frontier 与已经找到的 first solution。
 * 2. 是：结果对象不负责搜索，只承载 Reducer 输出。
 * 3. 是：返回 columns 时复制数组，避免共享可变结果。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 Driver 与 Reducer 之间的结果连接器。
 * 自行修改内容：新增不可变 ReduceResult。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
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
