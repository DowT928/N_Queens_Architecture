/*
 * 文件：CompleteSolutionCollectionFilter.java
 * 架构风格：管道-过滤器。
 * 本文件组件/连接器：完整解收集过滤器；向反馈管道或完整解管道发送消息。
 * 架构约束自查：
 * 1. 是：批次缓冲只属于本过滤器，发送时转为不可变副本。
 * 2. 是：不直接调用生成或输出过滤器。
 * 3. 是：未完成状态、首解和终止信号全部通过管道传递。
 * AI：OpenAI Codex 辅助实现并检查合法状态收集和终止消息处理；
 * 本人修改：核对完整解识别、未完成状态批次反馈、无解判断以及停止和失败消息的传递过程，并完善收集过滤器说明注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

import java.util.ArrayList;
import java.util.List;

import static nqueens.architectures.pipefilter.PipelineProtocol.*;

final class CompleteSolutionCollectionFilter implements Runnable {
    private final Pipe<LegalEvent> legalStatePipe;
    private final Pipe<FeedbackEvent> feedbackPipe;
    private final Pipe<ResultEvent> completeSolutionPipe;

    CompleteSolutionCollectionFilter(Pipe<LegalEvent> legalStatePipe,
                                     Pipe<FeedbackEvent> feedbackPipe,
                                     Pipe<ResultEvent> completeSolutionPipe) {
        this.legalStatePipe = legalStatePipe;
        this.feedbackPipe = feedbackPipe;
        this.completeSolutionPipe = completeSolutionPipe;
    }

    @Override
    public void run() {
        try {
            collect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            publishFailure(e);
        } catch (Throwable t) {
            publishFailure(t);
        }
    }

    private void collect() throws InterruptedException {
        List<SearchState> children = new ArrayList<>();
        SearchState firstSolution = null;

        while (true) {
            LegalEvent event = legalStatePipe.take();
            if (event instanceof LegalState legal) {
                SearchState state = legal.state();
                if (state.row() == state.n()) {
                    if (firstSolution == null) firstSolution = state;
                } else {
                    children.add(state);
                }
            } else if (event == LegalBatchEnd.INSTANCE) {
                if (firstSolution != null) {
                    completeSolutionPipe.put(new Solved(firstSolution.columns()));
                    feedbackPipe.put(FeedbackStop.INSTANCE);
                    return;
                }
                feedbackPipe.put(new ChildrenBatch(children));
                children = new ArrayList<>();
            } else if (event == LegalSearchExhausted.INSTANCE) {
                completeSolutionPipe.put(NoSolution.INSTANCE);
                return;
            } else if (event == LegalStop.INSTANCE) {
                return;
            } else if (event instanceof LegalFailure failure) {
                completeSolutionPipe.put(new ResultFailure(failure.cause()));
                feedbackPipe.put(FeedbackStop.INSTANCE);
                return;
            } else {
                throw new IllegalStateException("Unknown legal-state message: " + event);
            }
        }
    }

    private void publishFailure(Throwable cause) {
        try {
            completeSolutionPipe.put(new ResultFailure(cause));
            feedbackPipe.put(FeedbackStop.INSTANCE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
