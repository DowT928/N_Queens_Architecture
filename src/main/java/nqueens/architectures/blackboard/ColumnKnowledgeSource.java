/*
 * 文件：ColumnKnowledgeSource.java
 * 架构风格：黑板。
 * 本文件组件/连接器：Constraint Knowledge Source；检查列占用位向量。
 * 架构约束自查：
 * 1. 是：只读取黑板候选和列 mask，并把判断结果写回黑板。
 * 2. 是：不调用其他知识源。
 * 3. 是：使用统一的列占用 long 位向量剪枝。
 * AI：OpenAI Codex 辅助实现和检查位运算。
 * 本人修改：核对列下标和候选合法性判断。
 * 声明人：饶心翊
 */
package nqueens.architectures.blackboard;
final class ColumnKnowledgeSource implements KnowledgeSource { public void inspect(BlackboardStorage b){ b.columnAllowed=(b.occupiedColumns&(1L<<b.candidateColumn))==0; } }
