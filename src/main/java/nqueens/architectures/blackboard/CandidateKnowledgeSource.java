package nqueens.architectures.blackboard;
final class CandidateKnowledgeSource implements KnowledgeSource { public void inspect(BlackboardStorage b){ b.candidateAllowed=b.nextCandidate(); } }
