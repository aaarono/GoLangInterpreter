package parser.ast;

public class ParameterNode extends ASTNode {
    public String name;
    public TypeNode type;

    public ParameterNode(String name, TypeNode type, int line, int column) {
        super(line, column);
        this.name = name;
        this.type = type;
    }
}
