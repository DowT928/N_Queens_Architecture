/*
 * Architecture self-check: shared infrastructure; no search algorithm.
 * Shared mutable search state: none. Role: boundary/contract only.
 * AI: Codex generated scaffold. Owner review and handwritten declaration: pending.
 */
package nqueens.contract;

public final class ResultValidator {
    private ResultValidator() {}
    public static void validate(int n, SolveResult result) {
        if (n < 1 || n > 15 || result == null) throw new IllegalArgumentException("Invalid result");
        int[] c = result.columns();
        if (!result.found()) {
            if (c.length != 0 || (n != 2 && n != 3)) throw new IllegalArgumentException("Invalid no-solution result");
            return;
        }
        if (c.length != n) throw new IllegalArgumentException("Wrong board size");
        for (int row = 0; row < n; row++) {
            if (c[row] < 0 || c[row] >= n) throw new IllegalArgumentException("Column outside board");
            for (int prev = 0; prev < row; prev++) {
                if (c[prev] == c[row] || Math.abs(c[prev] - c[row]) == row - prev)
                    throw new IllegalArgumentException("Attacking queens");
            }
        }
    }
}
