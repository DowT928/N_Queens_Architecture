/*
 * 文件：CandidateRecord.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：Mapper 输出记录，也是 Shuffler 输入记录。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：每个候选位置都形成一条记录，不跳过 Shuffle 直接共享搜索状态。
 * 2. 是：记录携带 CandidateKey，支持合法组和冲突组真实分组。
 * 3. 是：记录携带 order，Reducer 可恢复 row/col 的统一首解顺序。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 MapReduce 阶段之间的记录流数据结构。
 * 自行修改内容：新增不可变记录，合法记录携带下一搜索状态。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
 */
package nqueens.architectures.mapreduce;

record CandidateRecord(CandidateKey key, int row, int col, long order, SearchState nextState) {
    CandidateRecord {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        if (key == CandidateKey.LEGAL && nextState == null) {
            throw new IllegalArgumentException("legal records must carry next state");
        }
    }
}
