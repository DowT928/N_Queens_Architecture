/*
 * Architecture self-check: shared infrastructure; no search algorithm.
 * Shared mutable search state: none. Role: boundary/contract only.
 * AI: Codex generated scaffold. Owner review and handwritten declaration: pending.
 */
package nqueens.contract;

public record SolveResult(boolean found, int[] columns) {
    public SolveResult {
        if (columns == null) throw new IllegalArgumentException("columns must not be null");
        columns = columns.clone();
    }
    @Override
    public int[] columns() { return columns.clone(); }
}
