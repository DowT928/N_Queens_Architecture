/*
 * 文件：Shuffler.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Shuffle 阶段。
 * 自查：真实按 key 分组；四类组都保留；不负责搜索扩展。
 * AI 使用情况：仅用于语法排错、概念/API 查询、代码审查和文字润色。
 * 本人工作：负责主要架构组织、框架实现、代码修改和运行验证。
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
