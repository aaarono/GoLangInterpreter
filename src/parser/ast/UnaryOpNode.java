package parser.ast;

public class UnaryOpNode extends ExpressionNode {
    public enum UnaryOp { PLUS, MINUS, NOT }

    public UnaryOp op;
    public ExpressionNode expr;

    public UnaryOpNode(UnaryOp op, ExpressionNode expr, int line, int column) {
        super(line, column);
        this.op = op;
        this.expr = expr;
    }
}

