/*
 * 文件：Pipe.java
 * 架构风格：管道-过滤器。
 * 本文件组件/连接器：基于阻塞队列的单向管道连接器。
 * 架构约束自查：
 * 1. 是：仅封装消息传递，不保存或解释搜索状态。
 * 2. 是：不调用任何过滤器的内部方法。
 * 3. 是：过滤器之间只通过本连接器读写消息。
 * AI：OpenAI Codex 辅助实现并检查阻塞队列封装和空消息校验；
 * 本人修改：核对 put/take 单向消息传递方式，确认连接器不解释、不修改搜索数据，并完善 Pipe 组件说明与自查注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class Pipe<T> {
    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();

    void put(T message) throws InterruptedException {
        queue.put(Objects.requireNonNull(message, "message"));
    }

    T take() throws InterruptedException {
        return queue.take();
    }
}
