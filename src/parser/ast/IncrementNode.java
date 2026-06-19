package parser.ast;

public class IncrementNode extends StatementNode {
    public ExpressionNode expr;

    public IncrementNode(ExpressionNode expr, int line, int column) {
        super(line, column);
        this.expr = expr;
    }
}
