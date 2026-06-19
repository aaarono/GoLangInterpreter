package parser.ast;

public class ValueNode extends ExpressionNode {
    public enum ValueType { INT, FLOAT, STRING }
    public ValueType valueType;
    public String value;

    public ValueNode(ValueType valueType, String value, int line, int column) {
        super(line, column);
        this.valueType = valueType;
        this.value = value;
    }
}
