/*
 * 文件：ArchitectureSolver.java
 * 架构风格：黑板。
 * 本文件组件/连接器：公共 Solver 入口；创建黑板、知识源和控制器。
 * 架构约束自查：
 * 1. 是：入口只负责创建本次 solve 的组件和返回公共结果。
 * 2. 是：可变状态不在构造函数中初始化，全部属于本次 solve。
 * 3. 是：输出交给公共 CLI，不直接打印诊断信息。
 * AI：OpenAI Codex 辅助实现、拆分组件和检查架构约束。
 * 本人修改：主要实现并核对架构图、公共接口、搜索顺序和三个位向量剪枝规则。
 * 声明人：饶心翊
 */
package nqueens.architectures.blackboard;

import nqueens.contract.SolveResult;
import nqueens.contract.Solver;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        if (n < 1 || n > 15) throw new IllegalArgumentException("n must be in [1, 15]");
        BlackboardStorage board = new BlackboardStorage(n);
        new Controller(board, new CandidateKnowledgeSource(), new ColumnKnowledgeSource(),
                new DiagonalKnowledgeSource(), new SolutionKnowledgeSource()).run();
        return board.result();
    }
}
