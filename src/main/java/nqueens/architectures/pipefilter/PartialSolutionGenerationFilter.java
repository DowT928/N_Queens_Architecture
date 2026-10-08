/*
 * 文件：PartialSolutionGenerationFilter.java
 * 架构风格：管道-过滤器。
 * 本文件组件/连接器：部分解生成过滤器；读取初始/反馈状态并输出候选位置。
 * 架构约束自查：
 * 1. 是：搜索栈只属于本过滤器，不与其他过滤器共享。
 * 2. 是：不直接调用合法性或收集过滤器。
 * 3. 是：全部输入输出均经初始、候选和反馈管道。
 * AI：OpenAI Codex 辅助实现并检查候选生成和反馈消息处理流程；
 * 本人修改：核对按列升序生成候选、批次结束标记和合法子状态的逆序入栈过程，确认搜索栈仅由本过滤器持有，并完善注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static nqueens.architectures.pipefilter.PipelineProtocol.*;

final class PartialSolutionGenerationFilter implements Runnable {
    private final Pipe<SearchState> initialStatePipe;
    private final Pipe<CandidateEvent> candidatePipe;
    private final Pipe<FeedbackEvent> feedbackPipe;

    PartialSolutionGenerationFilter(Pipe<SearchState> initialStatePipe,
                                    Pipe<CandidateEvent> candidatePipe,
                                    Pipe<FeedbackEvent> feedbackPipe) {
        this.initialStatePipe = initialStatePipe;
        this.candidatePipe = candidatePipe;
        this.feedbackPipe = feedbackPipe;
    }

    @Override
    public void run() {
        try {
            generate();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            publishFailure(e);
        } catch (Throwable t) {
            publishFailure(t);
        }
    }

    private void generate() throws InterruptedException {
        Deque<SearchState> stack = new ArrayDeque<>();
        stack.push(initialStatePipe.take());

        while (true) {
            if (stack.isEmpty()) {
                candidatePipe.put(CandidateSearchExhausted.INSTANCE);
                return;
            }

            SearchState parent = stack.pop();
            for (int col = 0; col < parent.n(); col++) {
                candidatePipe.put(new Candidate(parent, col));
            }
            candidatePipe.put(CandidateBatchEnd.INSTANCE);

            FeedbackEvent feedback = feedbackPipe.take();
            if (feedback instanceof ChildrenBatch batch) {
                pushInDepthFirstOrder(stack, batch.states());
            } else if (feedback == FeedbackStop.INSTANCE) {
                candidatePipe.put(CandidateStop.INSTANCE);
                return;
            } else {
                throw new IllegalStateException("Unknown feedback message: " + feedback);
            }
        }
    }

    private static void pushInDepthFirstOrder(Deque<SearchState> stack,
                                              List<SearchState> children) {
        for (int index = children.size() - 1; index >= 0; index--) {
            stack.push(children.get(index));
        }
    }

    private void publishFailure(Throwable cause) {
        try {
            candidatePipe.put(new CandidateFailure(cause));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
