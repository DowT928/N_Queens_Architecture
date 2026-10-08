/*
 * 文件：CandidateKey.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Shuffle 分组键。
 * 自查：包含合法组和三类冲突组；Mapper 标记，Shuffler 按 key 分组。
 * AI 使用声明：使用过 AI 辅助，已人工审核修改，详情见报告附录。
 * 声明人：江婧怡（手写签名另附）。
 */
package nqueens.architectures.mapreduce;

enum CandidateKey {
    LEGAL,
    COLUMN_CONFLICT,
    DIAG1_CONFLICT,
    DIAG2_CONFLICT
}
