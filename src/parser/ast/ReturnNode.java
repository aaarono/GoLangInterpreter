package parser.ast;

import java.util.List;

public class ReturnNode extends StatementNode {
    public List<ExpressionNode> exprs;

    public ReturnNode(List<ExpressionNode> exprs, int line, int column) {
        super(line, column);
        this.exprs = exprs;
    }
}
