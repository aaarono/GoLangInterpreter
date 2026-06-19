package parser.ast;

public class SliceTypeNode extends TypeNode {
    public TypeNode elementType;

    public SliceTypeNode(TypeNode elementType, int line, int column) {
        super(line, column);
        this.elementType = elementType;
    }
}
