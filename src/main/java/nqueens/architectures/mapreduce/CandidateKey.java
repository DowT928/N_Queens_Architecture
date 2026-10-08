/*
 * 文件：CandidateKey.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Shuffle 分组键。
 * 自查：包含合法组和三类冲突组；Mapper 标记，Shuffler 按 key 分组。
 * AI 使用情况：仅用于语法排错、概念/API 查询、代码审查和文字润色。
 * 本人工作：负责主要架构组织、框架实现、代码修改和运行验证。
 */
package nqueens.architectures.mapreduce;

enum CandidateKey {
    LEGAL,
    COLUMN_CONFLICT,
    DIAG1_CONFLICT,
    DIAG2_CONFLICT
}
