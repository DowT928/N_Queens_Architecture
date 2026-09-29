/* Shared contract tests, not an N-Queens solver. AI: Codex; owner review pending. */
import nqueens.contract.*;

public final class ContractChecks {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Contract check failed");
    }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected rejection");
    }
    public static void main(String[] args) {
        int[] source = {1, 3, 0, 2};
        SolveResult result = new SolveResult(true, source);
        source[0] = 0;
        check(result.columns()[0] == 1);
        int[] exposed = result.columns();
        exposed[0] = 0;
        check(result.columns()[0] == 1);
        ResultValidator.validate(4, result);
        ResultValidator.validate(1, new SolveResult(true, new int[]{0}));
        ResultValidator.validate(2, new SolveResult(false, new int[0]));
        ResultValidator.validate(3, new SolveResult(false, new int[0]));
        rejects(() -> new SolveResult(true, null));
        rejects(() -> ResultValidator.validate(4, null));
        rejects(() -> ResultValidator.validate(4, new SolveResult(false, new int[0])));
        rejects(() -> ResultValidator.validate(2, new SolveResult(false, new int[]{0})));
        rejects(() -> ResultValidator.validate(4, new SolveResult(true, new int[]{1})));
        rejects(() -> ResultValidator.validate(4, new SolveResult(true, new int[]{-1, 3, 0, 2})));
        rejects(() -> ResultValidator.validate(4, new SolveResult(true, new int[]{4, 3, 0, 2})));
        rejects(() -> ResultValidator.validate(4, new SolveResult(true, new int[]{1, 1, 0, 2})));
        rejects(() -> ResultValidator.validate(4, new SolveResult(true, new int[]{0, 1, 2, 3})));
        System.out.println("Contract checks passed");
    }
}
