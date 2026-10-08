/*
 * 文件：Shuffler.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：Shuffle 阶段。
 * 自查：真实按 key 分组；四类组都保留；不负责搜索扩展。
 * AI 使用声明：使用过 AI 辅助，已人工审核修改，详情见报告附录。
 * 声明人：江婧怡（手写签名另附）。
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
