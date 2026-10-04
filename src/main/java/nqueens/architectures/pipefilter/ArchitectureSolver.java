/*
 * 文件：ArchitectureSolver.java
 * 架构风格：管道-过滤器；负责人：杨铱玫。
 * 本文件组件/连接器：装配输入、生成、校验、收集和输出组件及五条管道。
 * 架构约束自查：
 * 1. 是：过滤器之间仅通过 Pipe 传递不可变消息。
 * 2. 是：过滤器不直接调用彼此的内部方法。
 * 3. 是：线程、队列、搜索和资源释放全部位于 solve 内。
 * AI：OpenAI Codex 辅助实现并检查组件装配与线程资源释放流程；
 * 本人修改：结合本组设计核对五条 Pipe 的连接关系，以及工作线程的启动、等待和异常结束流程，并完善组件职责与架构自查注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

import static nqueens.architectures.pipefilter.PipelineProtocol.*;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        if (n < 1 || n > 15) throw new IllegalArgumentException("N must be 1..15");

        Pipe<SearchState> initialStatePipe = new Pipe<>();
        Pipe<CandidateEvent> candidatePipe = new Pipe<>();
        Pipe<LegalEvent> legalStatePipe = new Pipe<>();
        Pipe<FeedbackEvent> feedbackPipe = new Pipe<>();
        Pipe<ResultEvent> completeSolutionPipe = new Pipe<>();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<?>> workers = new ArrayList<>();
        boolean completed = false;
        try {
            workers.add(executor.submit(new PartialSolutionGenerationFilter(
                    initialStatePipe, candidatePipe, feedbackPipe)));
            workers.add(executor.submit(new LegalityValidationFilter(
                    candidatePipe, legalStatePipe)));
            workers.add(executor.submit(new CompleteSolutionCollectionFilter(
                    legalStatePipe, feedbackPipe, completeSolutionPipe)));
            Future<SolveResult> output = executor.submit(
                    new ResultOutputFilter(completeSolutionPipe));

            new InputComponent(n, initialStatePipe).publish();
            SolveResult result = output.get();
            for (Future<?> worker : workers) worker.get();
            completed = true;
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Pipeline interrupted", e);
        } catch (ExecutionException e) {
            throw propagate(e.getCause());
        } finally {
            if (completed) {
                executor.shutdown();
            } else {
                for (Future<?> worker : workers) worker.cancel(true);
                executor.shutdownNow();
            }
            awaitTermination(executor);
        }
    }

    private static RuntimeException propagate(Throwable cause) {
        if (cause instanceof RuntimeException runtime) return runtime;
        if (cause instanceof Error error) throw error;
        return new IllegalStateException("Pipeline failed", cause);
    }

    private static void awaitTermination(ExecutorService executor) {
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS))
                    throw new IllegalStateException("Pipeline workers did not terminate");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
