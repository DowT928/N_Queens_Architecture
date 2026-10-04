package nqueens.architectures.blackboard;
final class Controller {
    private final BlackboardStorage b; private final KnowledgeSource candidate,column,diagonal; private final SolutionKnowledgeSource solution;
    Controller(BlackboardStorage b,KnowledgeSource candidate,KnowledgeSource column,KnowledgeSource diagonal,SolutionKnowledgeSource solution){this.b=b;this.candidate=candidate;this.column=column;this.diagonal=diagonal;this.solution=solution;}
    void run(){search(0);}
    private boolean search(int row){
        if(solution.complete(b,row)){b.status="solved";b.result=b.columns.clone();return true;}
        b.beginRow(row);
        while(true){candidate.inspect(b);if(!b.candidateAllowed)break;int c=b.candidateColumn;column.inspect(b);diagonal.inspect(b);solution.inspect(b);if(!b.candidateAllowed)continue;b.place();if(search(row+1))return true;b.remove(row,c);b.candidateRow=row;}
        return false;
    }
}
