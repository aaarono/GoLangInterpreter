package parser.ast;

public class BinaryOpNode extends ExpressionNode {
    public enum BinaryOp {
        OR, AND, EQ, NEQ, LT, GT, LEQ, GEQ, PLUS, MINUS, MULT, DIV, MOD
    }

    public ExpressionNode left;
    public ExpressionNode right;
    public BinaryOp op;

    public BinaryOpNode(ExpressionNode left, BinaryOp op, ExpressionNode right, int line, int column) {
        super(line, column);
        this.left = left;
        this.right = right;
        this.op = op;
    }
}
