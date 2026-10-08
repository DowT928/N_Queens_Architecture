/*
 * 文件：CandidateKey.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Shuffle 分组键。
 * 自查：包含合法组和三类冲突组；Mapper 标记，Shuffler 按 key 分组。
 */
package nqueens.architectures.mapreduce;

enum CandidateKey {
    LEGAL,
    COLUMN_CONFLICT,
    DIAG1_CONFLICT,
    DIAG2_CONFLICT
}
