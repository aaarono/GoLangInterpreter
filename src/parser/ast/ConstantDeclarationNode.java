package parser.ast;

import java.util.List;

public class ConstantDeclarationNode extends DeclarationNode {
    public List<String> idents;
    public List<ExpressionNode> values;

    public ConstantDeclarationNode(List<String> idents, List<ExpressionNode> values, int line, int column) {
        super(line, column);
        this.idents = idents;
        this.values = values;
    }
}
