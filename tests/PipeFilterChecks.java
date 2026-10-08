/*
 * 文件：PipeFilterChecks.java
 * 架构风格：管道-过滤器测试。
 * 本文件组件/连接器：验证公共契约、无解边界、确定性首解和重复运行一致性。
 * 架构约束自查：
 * 1. 是：测试只通过公共 Solver 入口观察结果。
 * 2. 是：不访问或调用任何过滤器内部方法。
 * 3. 是：不修改生产代码的状态或连接器。
 * AI：OpenAI Codex 辅助实现并检查测试覆盖范围和断言信息；
 * 本人修改：核对 N=1、2、3、4、8、10、12 的测试结果，检查首解合法性和重复运行一致性，并完善测试说明与断言提示。
 * 声明人：杨铱玫
 */
import java.util.Arrays;
import nqueens.architectures.pipefilter.ArchitectureSolver;
import nqueens.contract.ResultValidator;
import nqueens.contract.SolveResult;

public final class PipeFilterChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static SolveResult solveAndValidate(ArchitectureSolver solver, int n) {
        SolveResult result = solver.solve(n);
        ResultValidator.validate(n, result);
        return result;
    }

    public static void main(String[] args) {
        ArchitectureSolver solver = new ArchitectureSolver();
        check(Arrays.equals(solveAndValidate(solver, 1).columns(), new int[]{0}), "N=1");
        check(!solveAndValidate(solver, 2).found(), "N=2 must have no solution");
        check(!solveAndValidate(solver, 3).found(), "N=3 must have no solution");
        check(Arrays.equals(solveAndValidate(solver, 4).columns(), new int[]{1, 3, 0, 2}),
                "N=4 first solution");

        for (int n : new int[]{8, 10, 12}) {
            int[] first = solveAndValidate(solver, n).columns();
            int[] second = solveAndValidate(solver, n).columns();
            check(Arrays.equals(first, second), "First solution must be deterministic for N=" + n);
        }
        System.out.println("Pipe-filter checks passed");
    }
}
