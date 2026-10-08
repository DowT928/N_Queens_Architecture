/*
 * 文件：CandidateRecord.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Map 输出记录，作为 Shuffle 输入。
 * 自查：每个候选生成记录；记录携带 key 和 order；合法记录携带下一状态。
 * AI 使用情况：仅用于语法排错、概念/API 查询、代码审查和文字润色。
 * 本人工作：负责主要架构组织、框架实现、代码修改和运行验证。
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
