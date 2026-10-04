/*
 * 文件：ResultOutputFilter.java
 * 架构风格：管道-过滤器。
 * 本文件组件/连接器：结果输出过滤器；将终止消息转换为公共 SolveResult。
 * 架构约束自查：
 * 1. 是：不保存或共享可变搜索状态。
 * 2. 是：不直接调用其他过滤器，也不直接打印 stdout。
 * 3. 是：唯一输入来自完整解管道。
 * AI：OpenAI Codex 辅助实现并检查内部结果到公共结果的转换；
 * 本人修改：核对有解、无解和失败消息的处理分支，确认本过滤器不直接打印标准输出，并完善结果输出组件说明注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

import java.util.concurrent.Callable;
import nqueens.contract.SolveResult;

import static nqueens.architectures.pipefilter.PipelineProtocol.*;

final class ResultOutputFilter implements Callable<SolveResult> {
    private final Pipe<ResultEvent> completeSolutionPipe;

    ResultOutputFilter(Pipe<ResultEvent> completeSolutionPipe) {
        this.completeSolutionPipe = completeSolutionPipe;
    }

    @Override
    public SolveResult call() throws Exception {
        ResultEvent event = completeSolutionPipe.take();
        if (event instanceof Solved solved) {
            return new SolveResult(true, solved.columns());
        }
        if (event == NoSolution.INSTANCE) {
            return new SolveResult(false, new int[0]);
        }
        if (event instanceof ResultFailure failure) {
            Throwable cause = failure.cause();
            if (cause instanceof Exception exception) throw exception;
            throw new IllegalStateException("Pipeline failed", cause);
        }
        throw new IllegalStateException("Unknown result message: " + event);
    }
}
