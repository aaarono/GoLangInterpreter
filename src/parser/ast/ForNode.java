package parser.ast;

public class ForNode extends StatementNode {
    public ExpressionNode condition;
    public BlockNode body;

    public ForNode(ExpressionNode condition, BlockNode body, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.body = body;
    }
}
