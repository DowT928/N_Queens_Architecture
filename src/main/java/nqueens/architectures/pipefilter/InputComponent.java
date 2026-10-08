/*
 * 文件：InputComponent.java
 * 架构风格：管道-过滤器。
 * 本文件组件/连接器：输入组件，向初始状态管道写入空棋盘状态。
 * 架构约束自查：
 * 1. 是：不保存全局或共享可变搜索状态。
 * 2. 是：不直接调用任何过滤器。
 * 3. 是：输出仅写入初始状态管道。
 * AI：OpenAI Codex 辅助实现并检查输入组件的职责边界；
 * 本人修改：核对棋盘规模 N 的传入方式和初始搜索状态的发布过程，确认输入组件仅向初始状态管道写入数据，并完善说明注释。
 * 声明人：杨铱玫
 */
package nqueens.architectures.pipefilter;

final class InputComponent {
    private final int n;
    private final Pipe<SearchState> output;

    InputComponent(int n, Pipe<SearchState> output) {
        this.n = n;
        this.output = output;
    }

    void publish() throws InterruptedException {
        output.put(SearchState.initial(n));
    }
}
