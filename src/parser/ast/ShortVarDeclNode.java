package parser.ast;

import java.util.List;

public class ShortVarDeclNode extends StatementNode {
    public List<ExpressionNode> lhs;
    public List<ExpressionNode> exprs;

    public ShortVarDeclNode(List<ExpressionNode> lhs, List<ExpressionNode> exprs, int line, int column) {
        super(line, column);
        this.lhs = lhs;
        this.exprs = exprs;
    }
}


