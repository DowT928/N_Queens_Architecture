/*
 * 文件：Controller.java
 * 架构风格：黑板。
 * 本文件组件/连接器：控制器；调度 Candidate、Constraint、Solution 知识源。
 * 架构约束自查：
 * 1. 是：控制器负责知识源触发顺序和回溯调度。
 * 2. 是：知识源之间不直接调用，只由控制器协调。
 * 3. 是：按行递增、列递增搜索并保持首解确定性。
 * AI：OpenAI Codex 辅助实现、拆分组件和检查调度流程。
 * 本人修改：主要实现并核对知识源触发顺序、回退和线程外部无残留任务。
 * 声明人：饶心翊
 */
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
