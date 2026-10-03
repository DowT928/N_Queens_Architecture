/*
 * 文件：CandidateKey.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：Shuffle 分组键，区分合法候选和三类剪枝冲突。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：显式提供 LEGAL、COLUMN_CONFLICT、DIAG1_CONFLICT、DIAG2_CONFLICT 四类 key。
 * 2. 是：Mapper 只通过该 key 标记候选类别，Shuffler 按 key 真实分组。
 * 3. 是：Reducer 只消费 LEGAL 组，冲突组保留为 Shuffle 证据。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 MapReduce 候选记录分组契约。
 * 自行修改内容：新增枚举以支撑真实 Shuffle 阶段。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
 */
package nqueens.architectures.mapreduce;

enum CandidateKey {
    LEGAL,
    COLUMN_CONFLICT,
    DIAG1_CONFLICT,
    DIAG2_CONFLICT
}
