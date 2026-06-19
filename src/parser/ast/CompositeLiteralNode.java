package parser.ast;

import java.util.List;

public class CompositeLiteralNode extends ExpressionNode {
    public TypeNode elemType;
    public List<ExpressionNode> elements;
    public Integer length; // null для среза, не null для массива

    // Конструктор для среза
    public CompositeLiteralNode(TypeNode elemType, List<ExpressionNode> elements, int line, int column) {
        super(line, column);
        this.elemType = elemType;
        this.elements = elements;
        this.length = null;
    }

    // Конструктор для массива
    public CompositeLiteralNode(TypeNode elemType, List<ExpressionNode> elements, int line, int column, Integer length) {
        super(line, column);
        this.elemType = elemType;
        this.elements = elements;
        this.length = length;
    }
}
