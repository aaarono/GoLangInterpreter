package parser.ast;

public class ExpressionStatementNode extends StatementNode {
    public ExpressionNode expression;
    public ExpressionStatementNode(ExpressionNode expr, int line, int column) {
        super(line, column);
        this.expression = expr;
    }
}
