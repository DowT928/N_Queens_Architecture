/*
 * 文件：BlackboardStorage.java
 * 架构风格：黑板。
 * 本文件组件/连接器：黑板存储；保存 row、candidateColumn、queens、三个位向量、status 和 result。
 * 架构约束自查：
 * 1. 是：本文件实现共享黑板存储。
 * 2. 是：知识源之间不直接通信，只通过本黑板交换状态。
 * 3. 是：列和两条对角线使用统一的 long 位向量剪枝。
 * AI：OpenAI Codex 辅助实现、拆分组件和检查字段设计。
 * 本人修改：根据架构图补齐黑板字段并核对状态复制。
 * 声明人：饶心翊
 */
package nqueens.architectures.blackboard;
import nqueens.contract.SolveResult;
final class BlackboardStorage {
    final int n; final int[] columns; private final int[] cursors;
    long occupiedColumns, occupiedLeftDiagonals, occupiedRightDiagonals;
    int candidateRow, candidateColumn; boolean columnAllowed, diagonalAllowed, candidateAllowed;
    String status = "searching"; int[] result = new int[0];
    BlackboardStorage(int n) { this.n=n; columns=new int[n]; cursors=new int[n]; }
    void beginRow(int row) { candidateRow=row; cursors[row]=0; }
    boolean nextCandidate() { if(cursors[candidateRow]>=n)return false; candidateColumn=cursors[candidateRow]++; columnAllowed=diagonalAllowed=candidateAllowed=false; return true; }
    void place() { int r=candidateRow,c=candidateColumn; columns[r]=c; occupiedColumns|=1L<<c; occupiedLeftDiagonals|=1L<<(r-c+n-1); occupiedRightDiagonals|=1L<<(r+c); }
    void remove(int r,int c) { occupiedColumns^=1L<<c; occupiedLeftDiagonals^=1L<<(r-c+n-1); occupiedRightDiagonals^=1L<<(r+c); }
    SolveResult result() { return new SolveResult("solved".equals(status), result); }
}
