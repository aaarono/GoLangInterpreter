package parser.ast;

import java.util.List;

public class VariableDeclarationNode extends DeclarationNode {
    public List<String> idents;
    public TypeNode type; // nullable
    public List<ExpressionNode> initExprs;

    public VariableDeclarationNode(List<String> idents, TypeNode type, List<ExpressionNode> initExprs, int line, int column) {
        super(line, column);
        this.idents = idents;
        this.type = type;
        this.initExprs = initExprs;
    }
}
