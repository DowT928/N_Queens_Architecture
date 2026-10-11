/*
 * 文件：CandidateKnowledgeSource.java
 * 架构风格：黑板。
 * 本文件组件/连接器：Candidate Knowledge Source；按列递增向黑板提供候选位置。
 * 架构约束自查：
 * 1. 是：候选生成独立为知识源。
 * 2. 是：只通过黑板通信，不调用其他知识源。
 * 3. 是：候选顺序固定为每行列从小到大。
 * AI：OpenAI Codex 辅助实现和检查候选生成逻辑。
 * 本人修改：核对候选游标与回溯后的确定性顺序。
 * 声明人：饶心翊
 */
package nqueens.architectures.blackboard;
final class CandidateKnowledgeSource implements KnowledgeSource { public void inspect(BlackboardStorage b){ b.candidateAllowed=b.nextCandidate(); } }
