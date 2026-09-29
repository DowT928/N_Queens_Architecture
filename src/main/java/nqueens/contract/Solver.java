/*
 * Architecture self-check: shared infrastructure; no search algorithm.
 * Shared mutable search state: none. Role: boundary/contract only.
 * AI: Codex generated scaffold. Owner review and handwritten declaration: pending.
 */
package nqueens.contract;

public interface Solver {
    SolveResult solve(int n);
}
