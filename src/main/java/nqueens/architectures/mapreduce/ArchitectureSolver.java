/*
 * 文件：ArchitectureSolver.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：公共入口适配器。
 * 自查：入口只调用 Driver；构造函数不做求解；不写 stdout。
 * AI 使用声明：使用过 AI 辅助，已人工审核修改，详情见报告附录。
 * 声明人：江婧怡（手写签名另附）。
 */
package nqueens.architectures.mapreduce;

import nqueens.contract.Solver;
import nqueens.contract.SolveResult;

public final class ArchitectureSolver implements Solver {
    @Override
    public SolveResult solve(int n) {
        return new MapReduceDriver().solve(n);
    }
}
