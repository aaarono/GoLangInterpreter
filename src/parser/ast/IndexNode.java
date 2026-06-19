package parser.ast;

public class IndexNode extends ExpressionNode {
    public ExpressionNode arrayOrSlice;
    public ExpressionNode indexExpr;

    public IndexNode(ExpressionNode arrayOrSlice, ExpressionNode indexExpr, int line, int column) {
        super(line, column);
        this.arrayOrSlice = arrayOrSlice;
        this.indexExpr = indexExpr;
    }
}
