/* 文件：ArchitectureSolver.java；架构：黑板。
 * 组件：BlackboardStorage、Candidate/Constraint/Solution 三个独立知识源、Controller；知识源只经黑板通信。
 * 自查：是，具备黑板存储、独立知识源、控制器；按行列递增并使用三个位向量剪枝。
 * AI：Codex 辅助语法检查和约束审查；本人审核、修改及手写签名：待填写。 */
package nqueens.architectures.blackboard;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        if (n < 1 || n > 15) throw new IllegalArgumentException("n must be in [1, 15]");
        BlackboardStorage board = new BlackboardStorage(n);
        new Controller(board, new CandidateKnowledgeSource(), new ColumnKnowledgeSource(), new DiagonalKnowledgeSource(),
                new SolutionKnowledgeSource()).run();
        return board.result();
    }

    private static final class BlackboardStorage {
        private final int n; private final int[] columns; private final int[] candidateCursors;
        private long occupiedColumns, occupiedLeftDiagonals, occupiedRightDiagonals;
        private int candidateRow, candidateColumn;
        private boolean columnAllowed, diagonalAllowed, candidateAllowed;
        private String status = "searching";
        private int[] result = new int[0];
        private BlackboardStorage(int n) {
            this.n = n; columns = new int[n]; candidateCursors = new int[n];
        }
        private void beginRow(int row) { candidateRow = row; candidateCursors[row] = 0; }
        private boolean nextCandidate() {
            if (candidateCursors[candidateRow] >= n) return false;
            candidateColumn = candidateCursors[candidateRow]++;
            columnAllowed = diagonalAllowed = candidateAllowed = false;
            return true;
        }
        private void place() {
            int r = candidateRow, c = candidateColumn; columns[r] = c;
            occupiedColumns |= 1L << c;
            occupiedLeftDiagonals |= 1L << (r - c + n - 1);
            occupiedRightDiagonals |= 1L << (r + c);
        }
        private void remove(int r, int c) {
            occupiedColumns ^= 1L << c;
            occupiedLeftDiagonals ^= 1L << (r - c + n - 1);
            occupiedRightDiagonals ^= 1L << (r + c);
        }
        private SolveResult result() {
            return new SolveResult("solved".equals(status), result);
        }
    }
    private interface KnowledgeSource { void inspect(BlackboardStorage board); }
    private static final class CandidateKnowledgeSource implements KnowledgeSource {
        @Override public void inspect(BlackboardStorage b) {
            b.candidateAllowed = b.nextCandidate();
        }
    }
    private static final class ColumnKnowledgeSource implements KnowledgeSource {
        @Override public void inspect(BlackboardStorage b) {
            b.columnAllowed = (b.occupiedColumns & (1L << b.candidateColumn)) == 0;
        }
    }
    private static final class DiagonalKnowledgeSource implements KnowledgeSource {
        @Override public void inspect(BlackboardStorage b) {
            int r = b.candidateRow, c = b.candidateColumn;
            long left = 1L << (r - c + b.n - 1), right = 1L << (r + c);
            b.diagonalAllowed = (b.occupiedLeftDiagonals & left) == 0
                    && (b.occupiedRightDiagonals & right) == 0;
        }
    }
    private static final class SolutionKnowledgeSource implements KnowledgeSource {
        @Override public void inspect(BlackboardStorage b) {
            b.candidateAllowed = b.columnAllowed && b.diagonalAllowed;
        }
        private boolean complete(BlackboardStorage b, int row) { return row == b.n; }
    }
    private static final class Controller {
        private final BlackboardStorage board;
        private final KnowledgeSource candidateSource, columnSource, diagonalSource;
        private final SolutionKnowledgeSource solutionSource;
        private Controller(BlackboardStorage b, KnowledgeSource candidate, KnowledgeSource c, KnowledgeSource d,
                           SolutionKnowledgeSource s) {
            board = b; candidateSource = candidate; columnSource = c;
            diagonalSource = d; solutionSource = s;
        }
        private void run() { search(0); }
        private boolean search(int row) {
            if (solutionSource.complete(board, row)) {
                board.status = "solved"; board.result = board.columns.clone(); return true;
            }
            board.beginRow(row);
            while (true) {
                candidateSource.inspect(board);
                if (!board.candidateAllowed) break;
                int column = board.candidateColumn;
                columnSource.inspect(board); diagonalSource.inspect(board); solutionSource.inspect(board);
                if (!board.candidateAllowed) continue;
                board.place(); if (search(row + 1)) return true;
                board.remove(row, column); board.candidateRow = row;
            }
            return false;
        }
    }
}
