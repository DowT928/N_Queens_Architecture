/* ============================================================
 * 文件：CandidateRecord.java
 * 本文件实现的架构风格：MapReduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：Map 输出记录
 *   - 连接器：Mapper 与 Shuffler 之间的记录流
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 *   [是] 每个候选位置都形成记录，不绕过 Shuffle：是
 *   [是] 记录携带 key 和 order，支持真实分组与顺序恢复：是
 *   [是] 合法记录携带下一状态，冲突记录不生成下一状态：是
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：ChatGPT/Codex；用于任务：语法排错、概念学习查询、代码审查和文字润色；
 *   自行修改内容：本人负责主要架构组织、框架实现、代码修改和运行验证；声明人（手写签名）：江婧怡
 * ============================================================ */
package nqueens.architectures.mapreduce;

record CandidateRecord(CandidateKey key, int row, int col, long order, SearchState nextState) {
    CandidateRecord {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        if (key == CandidateKey.LEGAL && nextState == null) {
            throw new IllegalArgumentException("legal records must carry next state");
        }
    }
}
