package parser.ast;

public class IfNode extends StatementNode {
    public ExpressionNode condition;
    public StatementNode ifBlock;
    public StatementNode elseBlockOrIf;

    public IfNode(ExpressionNode condition, StatementNode ifBlock, StatementNode elseBlockOrIf, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.ifBlock = ifBlock;
        this.elseBlockOrIf = elseBlockOrIf;
    }
}
