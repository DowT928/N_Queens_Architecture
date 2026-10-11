/*
 * 文件：KnowledgeSource.java
 * 架构风格：黑板。
 * 本文件组件/连接器：知识源统一接口；规定知识源通过黑板读写状态。
 * 架构约束自查：
 * 1. 是：接口不保存搜索状态。
 * 2. 是：接口不建立知识源之间的直接调用关系。
 * 3. 是：所有知识源共享同一黑板通信方式。
 * AI：OpenAI Codex 辅助设计接口和检查组件边界。
 * 本人修改：核对接口只暴露黑板检查入口。
 * 声明人：饶心翊
 */
package nqueens.architectures.blackboard;
interface KnowledgeSource { void inspect(BlackboardStorage board); }
