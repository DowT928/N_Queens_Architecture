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
