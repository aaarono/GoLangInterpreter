package parser.ast;

import lexer.TokenType;
import java.util.List;

public class AssignmentNode extends StatementNode {
    public List<ExpressionNode> lhs; // вместо idents
    public TokenType op;
    public List<ExpressionNode> values;

    public AssignmentNode(List<ExpressionNode> lhs, TokenType op, List<ExpressionNode> values, int line, int column) {
        super(line, column);
        this.lhs = lhs;
        this.op = op;
        this.values = values;
    }
}
