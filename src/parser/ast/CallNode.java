package parser.ast;

import java.util.List;

public class CallNode extends ExpressionNode {
    public ExpressionNode function;
    public List<ExpressionNode> arguments;

    public CallNode(ExpressionNode function, List<ExpressionNode> arguments, int line, int column) {
        super(line, column);
        this.function = function;
        this.arguments = arguments;
    }
}
