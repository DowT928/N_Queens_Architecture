/* ============================================================
 * 文件：ReduceResult.java
 * 本文件实现的架构风格：MapReduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：Reduce 阶段输出结果
 *   - 连接器：Reducer 向 MapReduceDriver 返回下一轮 frontier 或首解
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 *   [是] 本文件区分继续搜索的 frontier 与已找到的首解：是
 *   [是] 本文件不执行搜索和剪枝，只承载 Reduce 输出：是
 *   [是] 本文件返回 columns 时使用复制结果，避免暴露可变数组：是
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：ChatGPT/Codex；用于任务：语法排错、概念/API 查询、代码审查和文字润色；
 *   自行修改内容：本人负责主要架构组织、框架实现、代码修改和运行验证；声明人（手写签名）：江婧怡
 * ============================================================ */
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
