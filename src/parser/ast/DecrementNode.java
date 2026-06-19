package parser.ast;

public class DecrementNode extends StatementNode {
    public ExpressionNode expr;

    public DecrementNode(ExpressionNode expr, int line, int column) {
        super(line, column);
        this.expr = expr;
    }
}
