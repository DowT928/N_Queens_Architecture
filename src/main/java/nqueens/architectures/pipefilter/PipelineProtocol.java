/*
 * 文件：PipelineProtocol.java
 * 架构风格：管道-过滤器。
 * 本文件组件/连接器：五条管道使用的不可变消息协议。
 * 架构约束自查：
 * 1. 是：消息对数组和列表进行防御性复制。
 * 2. 是：协议不调用任何过滤器。
 * 3. 是：批次、终止和失败均通过管道消息表达。
 * AI：OpenAI Codex 辅助实现并检查管道消息分类和防御性复制；
 * 本人修改：核对候选、批次结束、反馈、停止和失败消息的传递边界，检查数组及列表复制方式，并完善协议说明注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

import java.util.List;
import java.util.Objects;

final class PipelineProtocol {
    private PipelineProtocol() {}

    sealed interface CandidateEvent permits Candidate, CandidateBatchEnd,
            CandidateSearchExhausted, CandidateStop, CandidateFailure {}

    record Candidate(SearchState parent, int column) implements CandidateEvent {
        Candidate {
            Objects.requireNonNull(parent, "parent");
        }
    }

    enum CandidateBatchEnd implements CandidateEvent { INSTANCE }
    enum CandidateSearchExhausted implements CandidateEvent { INSTANCE }
    enum CandidateStop implements CandidateEvent { INSTANCE }

    record CandidateFailure(Throwable cause) implements CandidateEvent {
        CandidateFailure {
            Objects.requireNonNull(cause, "cause");
        }
    }

    sealed interface LegalEvent permits LegalState, LegalBatchEnd,
            LegalSearchExhausted, LegalStop, LegalFailure {}

    record LegalState(SearchState state) implements LegalEvent {
        LegalState {
            Objects.requireNonNull(state, "state");
        }
    }

    enum LegalBatchEnd implements LegalEvent { INSTANCE }
    enum LegalSearchExhausted implements LegalEvent { INSTANCE }
    enum LegalStop implements LegalEvent { INSTANCE }

    record LegalFailure(Throwable cause) implements LegalEvent {
        LegalFailure {
            Objects.requireNonNull(cause, "cause");
        }
    }

    sealed interface FeedbackEvent permits ChildrenBatch, FeedbackStop {}

    record ChildrenBatch(List<SearchState> states) implements FeedbackEvent {
        ChildrenBatch {
            states = List.copyOf(Objects.requireNonNull(states, "states"));
        }
    }

    enum FeedbackStop implements FeedbackEvent { INSTANCE }

    sealed interface ResultEvent permits Solved, NoSolution, ResultFailure {}

    record Solved(int[] columns) implements ResultEvent {
        Solved {
            columns = Objects.requireNonNull(columns, "columns").clone();
        }

        @Override
        public int[] columns() {
            return columns.clone();
        }
    }

    enum NoSolution implements ResultEvent { INSTANCE }

    record ResultFailure(Throwable cause) implements ResultEvent {
        ResultFailure {
            Objects.requireNonNull(cause, "cause");
        }
    }
}
