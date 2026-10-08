/* 文件：ArchitectureSolver.java；架构：黑板；组件：Blackboard、Candidate KS、Constraint KS、Solution KS、Controller。 */
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
