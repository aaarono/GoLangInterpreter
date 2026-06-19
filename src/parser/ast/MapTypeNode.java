package parser.ast;

public class MapTypeNode extends TypeNode {
    public TypeNode keyType;
    public TypeNode valueType;

    public MapTypeNode(TypeNode keyType, TypeNode valueType, int line, int column) {
        super(line, column);
        this.keyType = keyType;
        this.valueType = valueType;
    }
}
