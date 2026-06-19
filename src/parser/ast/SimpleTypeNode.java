package parser.ast;

public class SimpleTypeNode extends TypeNode {
    public String typeName;

    public SimpleTypeNode(String typeName, int line, int column) {
        super(line, column);
        this.typeName = typeName;
    }
}
