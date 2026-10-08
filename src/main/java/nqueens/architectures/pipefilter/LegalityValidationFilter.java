/*
 * 文件：LegalityValidationFilter.java
 * 架构风格：管道-过滤器。
 * 本文件组件/连接器：合法性校验过滤器；检查列和两条对角线位向量。
 * 架构约束自查：
 * 1. 是：不保存或共享可变棋盘状态。
 * 2. 是：不直接调用生成或收集过滤器。
 * 3. 是：候选从管道读取，合法状态写入下游管道。
 * AI：OpenAI Codex 辅助实现并检查位向量冲突判断；
 * 本人修改：核对列、row-col+n-1 和 row+col 三个位索引公式，检查非法候选剪枝及合法状态生成过程，并完善校验说明注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

import static nqueens.architectures.pipefilter.PipelineProtocol.*;

final class LegalityValidationFilter implements Runnable {
    private final Pipe<CandidateEvent> candidatePipe;
    private final Pipe<LegalEvent> legalStatePipe;

    LegalityValidationFilter(Pipe<CandidateEvent> candidatePipe,
                             Pipe<LegalEvent> legalStatePipe) {
        this.candidatePipe = candidatePipe;
        this.legalStatePipe = legalStatePipe;
    }

    @Override
    public void run() {
        try {
            validateCandidates();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            publishFailure(e);
        } catch (Throwable t) {
            publishFailure(t);
        }
    }

    private void validateCandidates() throws InterruptedException {
        while (true) {
            CandidateEvent event = candidatePipe.take();
            if (event instanceof Candidate candidate) {
                SearchState child = validate(candidate.parent(), candidate.column());
                if (child != null) legalStatePipe.put(new LegalState(child));
            } else if (event == CandidateBatchEnd.INSTANCE) {
                legalStatePipe.put(LegalBatchEnd.INSTANCE);
            } else if (event == CandidateSearchExhausted.INSTANCE) {
                legalStatePipe.put(LegalSearchExhausted.INSTANCE);
                return;
            } else if (event == CandidateStop.INSTANCE) {
                legalStatePipe.put(LegalStop.INSTANCE);
                return;
            } else if (event instanceof CandidateFailure failure) {
                legalStatePipe.put(new LegalFailure(failure.cause()));
                return;
            } else {
                throw new IllegalStateException("Unknown candidate message: " + event);
            }
        }
    }

    private static SearchState validate(SearchState parent, int col) {
        int row = parent.row();
        if (row >= parent.n() || col < 0 || col >= parent.n())
            throw new IllegalArgumentException("Candidate outside board");

        long columnBit = 1L << col;
        long diag1Bit = 1L << (row - col + parent.n() - 1);
        long diag2Bit = 1L << (row + col);
        if ((parent.columnsMask() & columnBit) != 0L
                || (parent.diag1Mask() & diag1Bit) != 0L
                || (parent.diag2Mask() & diag2Bit) != 0L) {
            return null;
        }

        int[] columns = parent.columns();
        columns[row] = col;
        return new SearchState(parent.n(), row + 1, columns,
                parent.columnsMask() | columnBit,
                parent.diag1Mask() | diag1Bit,
                parent.diag2Mask() | diag2Bit);
    }

    private void publishFailure(Throwable cause) {
        try {
            legalStatePipe.put(new LegalFailure(cause));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
