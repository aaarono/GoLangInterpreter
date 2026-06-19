package parser.ast;

public class ArrayTypeNode extends TypeNode {
    public Integer length; // Может быть null, если не указано
    public TypeNode elementType;

    public ArrayTypeNode(Integer length, TypeNode elementType, int line, int column) {
        super(line, column);
        this.length = length;
        this.elementType = elementType;
    }
}

