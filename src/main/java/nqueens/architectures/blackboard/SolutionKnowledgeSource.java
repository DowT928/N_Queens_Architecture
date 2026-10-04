package nqueens.architectures.blackboard;
final class SolutionKnowledgeSource implements KnowledgeSource { public void inspect(BlackboardStorage b){ b.candidateAllowed=b.columnAllowed&&b.diagonalAllowed; } boolean complete(BlackboardStorage b,int row){ return row==b.n; } }
