/* ============================================================
 * 文件：ArchitectureSolver.java
 * 本文件实现的架构风格：MapReduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：公共 Solver 入口适配器
 *   - 连接器：连接公共 CLI 与 MapReduceDriver
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 *   [是] 本文件不直接实现搜索细节，只调用 MapReduceDriver：是
 *   [是] 构造函数不做求解、不初始化线程或队列资源：是
 *   [是] 本文件不写 stdout，结果输出交给公共 CLI：是
 * ------------------------------------------------------------
 * AI 使用情况声明：
 *   使用工具：ChatGPT/Codex；用于任务：语法排错、概念学习查询、代码审查和文字润色；
 *   自行修改内容：本人负责主要架构组织、框架实现、代码修改和运行验证；声明人（手写签名）：江婧怡
 * ============================================================ */
package nqueens.architectures.mapreduce;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        return new MapReduceDriver().solve(n);
    }
}
