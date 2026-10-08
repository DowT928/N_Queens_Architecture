/*
 * 文件：ArchitectureSolver.java
 * 架构风格：MapReduce（单机模拟）
 * 组件/连接器：公共入口适配器。
 * 自查：入口只调用 Driver；构造函数不做求解；不写 stdout。
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
