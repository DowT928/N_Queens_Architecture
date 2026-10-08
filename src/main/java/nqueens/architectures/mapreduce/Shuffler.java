/* ============================================================
 * 文件：Shuffler.java
 * 本文件实现的架构风格：MapReduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：Shuffle 阶段组件
 *   - 连接器：把 CandidateRecord 记录流转换为按 CandidateKey 分组的结果
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 *   [是] 本文件真实构造按 key 分组的 Map 结果：是
 *   [是] 本文件保留 LEGAL 和三类冲突组结构：是
 *   [是] 本文件不执行搜索扩展，首解判断交给 Reducer：是
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：ChatGPT/Codex；用于任务：语法排错、概念学习查询、代码审查和文字润色；
 *   自行修改内容：本人负责主要架构组织、框架实现、代码修改和运行验证；声明人（手写签名）：江婧怡
 * ============================================================ */
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
