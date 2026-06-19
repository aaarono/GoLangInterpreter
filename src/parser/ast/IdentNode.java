package parser.ast;

public class IdentNode extends ExpressionNode {
    public String name;

    public IdentNode(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }
}
