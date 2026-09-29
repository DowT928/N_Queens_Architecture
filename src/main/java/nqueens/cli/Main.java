/*
 * Architecture self-check: shared infrastructure; no search algorithm.
 * Shared mutable search state: none. Role: boundary/contract only.
 * AI: Codex generated scaffold. Owner review and handwritten declaration: pending.
 */
package nqueens.cli;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import nqueens.contract.*;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        System.exit(run(args));
    }
    private static int run(String[] args) {
        String architecture;
        int n;
        Solver solver;
        try {
            if (args.length != 4 || !args[0].equals("--architecture") || !args[2].equals("--n"))
                throw new IllegalArgumentException("Usage: --architecture <id> --n <1..15>");
            architecture = args[1];
            n = Integer.parseInt(args[3]);
            if (n < 1 || n > 15) throw new IllegalArgumentException("N must be 1..15");
            solver = switch (architecture) {
                case "pipefilter" -> new nqueens.architectures.pipefilter.ArchitectureSolver();
                case "callreturn-iterative" -> new nqueens.architectures.callreturniterative.ArchitectureSolver();
                case "callreturn-backtracking" -> new nqueens.architectures.callreturnbacktracking.ArchitectureSolver();
                case "blackboard" -> new nqueens.architectures.blackboard.ArchitectureSolver();
                case "mapreduce" -> new nqueens.architectures.mapreduce.ArchitectureSolver();
                default -> throw new IllegalArgumentException("Unknown architecture: " + architecture);
            };
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
        long start = System.nanoTime();
        long elapsed;
        SolveResult result;
        try {
            result = solver.solve(n);
            elapsed = System.nanoTime() - start;
            ResultValidator.validate(n, result);
        } catch (UnsupportedOperationException e) {
            System.err.println(e.getMessage());
            emit(architecture, n, "not_implemented", new int[0], System.nanoTime() - start);
            return 3;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            emit(architecture, n, "error", new int[0], System.nanoTime() - start);
            return 1;
        }
        emit(architecture, n, result.found() ? "solved" : "no_solution", result.columns(), elapsed);
        return 0;
    }
    private static void emit(String architecture, int n, String status, int[] columns, long elapsed) {
        System.out.printf("{\"protocol_version\":1,\"architecture\":\"%s\",\"n\":%d,\"mode\":\"first\",\"status\":\"%s\",\"columns\":%s,\"solve_elapsed_ns\":%d}%n",
                architecture, n, status, Arrays.toString(columns), elapsed);
    }
}
