/*
 * 文件：Shuffler.java
 * 架构风格：MapReduce（单机模拟）
 * 本文件组件/连接器：Shuffle 阶段组件，把候选记录按 CandidateKey 分组。
 * 架构约束自查（每项填是/否及说明）：
 * 1. 是：真实构造 EnumMap<CandidateKey, List<CandidateRecord>> 分组结果。
 * 2. 是：四类 key 都初始化为空组，即使某轮没有该类记录也保留组结构。
 * 3. 是：不执行搜索扩展，合法状态生成和首解判断留给 Reducer。
 * AI 工具及版本：Codex（GPT-5）
 * 用于任务：实现 MapReduce 的 Shuffle 阶段。
 * 自行修改内容：新增按 key 分组的连接器实现。
 * 声明人及任务书要求的手写签名：待江婧怡线下补充。
 */
package nqueens.architectures.mapreduce;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class Shuffler {
    Map<CandidateKey, List<CandidateRecord>> shuffle(List<CandidateRecord> records) {
        Map<CandidateKey, List<CandidateRecord>> grouped = new EnumMap<>(CandidateKey.class);
        for (CandidateKey key : CandidateKey.values()) {
            grouped.put(key, new ArrayList<>());
        }
        for (CandidateRecord record : records) {
            grouped.get(record.key()).add(record);
        }
        return grouped;
    }
}
