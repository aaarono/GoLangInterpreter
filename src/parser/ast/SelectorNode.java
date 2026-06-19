package parser.ast;

public class SelectorNode extends ExpressionNode {
    public ExpressionNode object;
    public String fieldName;

    public SelectorNode(ExpressionNode object, String fieldName, int line, int column) {
        super(line, column);
        this.object = object;
        this.fieldName = fieldName;
    }
}
