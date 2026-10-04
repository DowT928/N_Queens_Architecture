package nqueens.architectures.blackboard;
final class DiagonalKnowledgeSource implements KnowledgeSource { public void inspect(BlackboardStorage b){ int r=b.candidateRow,c=b.candidateColumn; long l=1L<<(r-c+b.n-1),q=1L<<(r+c); b.diagonalAllowed=(b.occupiedLeftDiagonals&l)==0&&(b.occupiedRightDiagonals&q)==0; } }
