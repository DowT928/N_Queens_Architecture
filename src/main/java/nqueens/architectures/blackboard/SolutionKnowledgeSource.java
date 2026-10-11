/*
 * 文件：SolutionKnowledgeSource.java
 * 架构风格：黑板。
 * 本文件组件/连接器：Solution Knowledge Source；汇总约束结果并判断完整解。
 * 架构约束自查：
 * 1. 是：只通过黑板读取列和对角线判断结果。
 * 2. 是：不调用其他知识源。
 * 3. 是：完整解判断只以行数为依据，不改变统一搜索规则。
 * AI：OpenAI Codex 辅助实现和检查解判定流程。
 * 本人修改：核对候选汇总、完整解状态和结果写入。
 * 声明人：饶心翊
 */
package nqueens.architectures.blackboard;
final class SolutionKnowledgeSource implements KnowledgeSource { public void inspect(BlackboardStorage b){ b.candidateAllowed=b.columnAllowed&&b.diagonalAllowed; } boolean complete(BlackboardStorage b,int row){ return row==b.n; } }
