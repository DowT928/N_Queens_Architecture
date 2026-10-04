package nqueens.architectures.blackboard;
final class ColumnKnowledgeSource implements KnowledgeSource { public void inspect(BlackboardStorage b){ b.columnAllowed=(b.occupiedColumns&(1L<<b.candidateColumn))==0; } }
