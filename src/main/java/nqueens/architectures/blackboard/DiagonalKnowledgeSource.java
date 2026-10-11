/*
 * 文件：DiagonalKnowledgeSource.java
 * 架构风格：黑板。
 * 本文件组件/连接器：Constraint Knowledge Source；检查两条对角线占用位向量。
 * 架构约束自查：
 * 1. 是：检查 row-column+n-1 和 row+column 两条对角线。
 * 2. 是：只通过黑板通信，不调用其他知识源。
 * 3. 是：不使用对称性优化或其他额外剪枝。
 * AI：OpenAI Codex 辅助实现和检查对角线索引公式。
 * 本人修改：核对两条对角线位移及边界范围。
 * 声明人：饶心翊
 */
package nqueens.architectures.blackboard;
final class DiagonalKnowledgeSource implements KnowledgeSource { public void inspect(BlackboardStorage b){ int r=b.candidateRow,c=b.candidateColumn; long l=1L<<(r-c+b.n-1),q=1L<<(r+c); b.diagonalAllowed=(b.occupiedLeftDiagonals&l)==0&&(b.occupiedRightDiagonals&q)==0; } }
